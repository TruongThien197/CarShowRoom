package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CarModelRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.repository.RefundTransactionRepository;
import com.hsf302.carshowroom.service.BookingService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.ProductService;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.RefundPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final CarModelRepository carModelRepository;
    private final OrderService orderService;
    private final BookingService bookingService;
    private final ProductService productService;
    private final AuthService authService;
    private final RefundPayoutService refundPayoutService;

    @GetMapping
    public String dashboard(Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingDate"));
        model.addAttribute("pendingOrders", countOrderStatus(orders, "PENDING_PAYMENT"));
        model.addAttribute("pendingBookings", countBookingStatus(bookings, "PENDING_PAYMENT"));
        model.addAttribute("completedBookings", countBookingStatus(bookings, "COMPLETED"));
        return "staff/dashboard";
    }

    @GetMapping("/bookings")
    public String bookings(Model model) {
        List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingDate"));
        Map<Integer, String> bookingServices = bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingServiceRepository.findByBookingId(booking.getId()).stream()
                        .map(service -> service.getService() != null
                                ? service.getService().getServiceName()
                                : service.getServiceNameSnapshot())
                        .collect(Collectors.joining(", "))
        ));
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", bookingServices);
        return "staff/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetail(@org.springframework.web.bind.annotation.PathVariable Integer id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));
        model.addAttribute("booking", booking);
        model.addAttribute("bookingServices", bookingServiceRepository.findByBookingId(id));
        model.addAttribute("paymentTransactions", paymentTransactionRepository.findByBooking(booking));
        model.addAttribute("refundTransactions", refundTransactionRepository.findByBookingOrderByRequestedAtDesc(booking));
        return "staff/booking-detail";
    }

    @GetMapping("/parts")
    public String parts(@RequestParam(required = false) String brand,
                        @RequestParam(required = false) Integer carModelId,
                        @RequestParam(required = false) String keyword,
                        Model model) {
        Page<Product> results = productService.findProductsPaged(null, keyword, carModelId, brand, null, null, PageRequest.of(0, 50));
        model.addAttribute("products", results.getContent());
        model.addAttribute("carBrands", carModelRepository.findDistinctBrands());
        model.addAttribute("carModels", carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc());
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("selectedCarModelId", carModelId);
        model.addAttribute("keyword", keyword);
        return "staff/parts";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        Map<Integer, String> orderItems = orders.stream().collect(Collectors.toMap(
                Order::getId,
                order -> orderItemRepository.findByOrderId(order.getId()).stream()
                        .map(item -> item.getProductNameSnapshot() + " x" + item.getQuantity())
                        .collect(Collectors.joining(", "))
        ));
        model.addAttribute("orders", orders);
        model.addAttribute("orderItems", orderItems);
        return "staff/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderItemRepository.findByOrderId(id));
        model.addAttribute("refundTransactions", refundTransactionRepository.findByOrderOrderByRequestedAtDesc(order));
        return "staff/order-detail";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Integer orderId,
                                    @RequestParam String status,
                                    @RequestParam(required = false) String shippingCarrier,
                                    @RequestParam(required = false) String trackingCode,
                                    RedirectAttributes redirectAttributes) {
        try {
            String nextStatus = status.toUpperCase();
            Order order = orderService.getOrderById(orderId);
            if (!Set.of("PROCESSING", "SHIPPING", "COMPLETED").contains(nextStatus)) {
                throw new IllegalArgumentException("Nhân viên chỉ được xử lý đơn theo các trạng thái: Đang xử lý, Đang giao và Hoàn tất.");
            }
            if (!canStaffMoveOrder(order, nextStatus)) {
                throw new IllegalStateException("Đơn hàng không thể chuyển sang trạng thái đã chọn theo tiến trình hiện tại.");
            }
            if ("SHIPPING".equals(nextStatus)) {
                orderService.updateShipment(orderId, shippingCarrier, trackingCode);
            } else {
                orderService.updateOrderStatus(orderId, nextStatus);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật đơn hàng: " + e.getMessage());
        }
        return "redirect:/staff/orders/" + orderId;
    }

    @PostMapping("/bookings/status")
    public String updateBookingStatus(@RequestParam Integer bookingId,
                                      @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (!Set.of("IN_PROGRESS", "COMPLETED", "CANCELED").contains(status.toUpperCase())) {
                throw new IllegalArgumentException("Trạng thái lịch hẹn không hợp lệ.");
            }
            bookingService.updateStatus(bookingId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái lịch hẹn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật lịch hẹn: " + e.getMessage());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    @PostMapping("/bookings/check-in")
    public String checkInBooking(@RequestParam Integer bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.checkIn(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tiếp nhận xe và chuyển lịch sang đang thực hiện.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    @PostMapping("/bookings/final-amount")
    public String setFinalAmount(@RequestParam Integer bookingId, @RequestParam BigDecimal finalAmount,
                                 RedirectAttributes redirectAttributes) {
        try {
            bookingService.setFinalAmount(bookingId, finalAmount);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu giá cuối của dịch vụ.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    @PostMapping("/bookings/labor-collection")
    public String recordLaborCollection(@RequestParam Integer bookingId,
                                        @RequestParam BigDecimal laborFee,
                                        RedirectAttributes redirectAttributes) {
        try {
            bookingService.recordLaborCollection(bookingId, authService.getCurrentUser(), laborFee);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận tiền công lắp đặt đã thu tại xưởng.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    @PostMapping("/bookings/no-show")
    public String markBookingNoShow(@RequestParam Integer bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.markNoShow(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu khách không đến.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    @PostMapping("/bookings/refund")
    public String completeBookingRefund(@RequestParam Integer bookingId,
                                        @RequestParam String bankName,
                                        @RequestParam String bankBin,
                                        @RequestParam String accountHolder,
                                        @RequestParam String accountNumber,
                                        @RequestParam String note,
                                        RedirectAttributes redirectAttributes) {
        try {
            bookingService.completeRefund(bookingId, authService.getCurrentUser(),
                    bankName, bankBin, accountHolder, accountNumber, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo lệnh chi PayOS cho hoàn tiền cọc.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    @PostMapping("/orders/refund")
    public String completeRefund(@RequestParam Integer orderId,
                                 @RequestParam String bankName,
                                 @RequestParam String bankBin,
                                 @RequestParam String accountHolder,
                                 @RequestParam String accountNumber,
                                 @RequestParam String note,
                                 RedirectAttributes redirectAttributes) {
        try {
            orderService.completeRefund(orderId, authService.getCurrentUser(), bankName, bankBin, accountHolder, accountNumber, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo lệnh chi PayOS cho hoàn tiền.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/staff/orders/" + orderId;
    }

    @PostMapping("/orders/{id}/cancellation/approve")
    public String approveOrderCancellation(@PathVariable Integer id, RedirectAttributes attributes) {
        try { orderService.approveCancellation(id, authService.getCurrentUser()); attributes.addFlashAttribute("successMessage", "Đã duyệt yêu cầu hủy đơn."); }
        catch (Exception exception) { attributes.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/staff/orders/" + id;
    }

    @PostMapping("/orders/{id}/cancellation/reject")
    public String rejectOrderCancellation(@PathVariable Integer id, @RequestParam String reason, RedirectAttributes attributes) {
        try { orderService.rejectCancellation(id, authService.getCurrentUser(), reason); attributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu hủy đơn."); }
        catch (Exception exception) { attributes.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/staff/orders/" + id;
    }

    @PostMapping("/bookings/{id}/cancellation/approve")
    public String approveBookingCancellation(@PathVariable Integer id, @RequestParam(required = false) String assessmentNote, RedirectAttributes attributes) {
        try { bookingService.approveCancellation(id, authService.getCurrentUser(), assessmentNote); attributes.addFlashAttribute("successMessage", "Đã duyệt yêu cầu hủy lịch."); }
        catch (Exception exception) { attributes.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/staff/bookings/" + id;
    }

    @PostMapping("/bookings/{id}/cancellation/reject")
    public String rejectBookingCancellation(@PathVariable Integer id, @RequestParam String reason, RedirectAttributes attributes) {
        try { bookingService.rejectCancellation(id, authService.getCurrentUser(), reason); attributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu hủy lịch."); }
        catch (Exception exception) { attributes.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/staff/bookings/" + id;
    }

    @PostMapping("/refunds/{id}/sync")
    public String syncRefund(@PathVariable Long id,
                             @RequestParam String returnUrl,
                             RedirectAttributes redirectAttributes) {
        try {
            refundPayoutService.syncRefundTransaction(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đồng bộ trạng thái lệnh chi PayOS.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:" + safeLocalReturnUrl(returnUrl);
    }

    private String safeLocalReturnUrl(String returnUrl) {
        return returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//") ? returnUrl : "/staff";
    }

    private long countOrderStatus(List<Order> orders, String status) {
        return orders.stream()
                .filter(order -> status.equalsIgnoreCase(order.getOrderStatus().name()))
                .count();
    }

    private boolean canStaffMoveOrder(Order order, String nextStatus) {
        return switch (order.getOrderStatus()) {
            case PENDING_PAYMENT, CREATED -> "PROCESSING".equals(nextStatus);
            case PROCESSING -> (order.getOrderType().name().equals("SHIPPING") && "SHIPPING".equals(nextStatus))
                    || (!order.getOrderType().name().equals("SHIPPING") && "COMPLETED".equals(nextStatus));
            case SHIPPING -> "SHIPPING".equals(nextStatus) || "COMPLETED".equals(nextStatus);
            default -> false;
        };
    }

    private long countBookingStatus(List<Booking> bookings, String status) {
        return bookings.stream()
                .filter(booking -> status.equalsIgnoreCase(booking.getBookingStatus().name()))
                .count();
    }
}
