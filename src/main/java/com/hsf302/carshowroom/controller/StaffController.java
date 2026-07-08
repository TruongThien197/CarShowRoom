package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.CarModel;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CarModelRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.service.BookingService;
import com.hsf302.carshowroom.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final CarModelRepository carModelRepository;
    private final OrderService orderService;
    private final BookingService bookingService;

    @GetMapping
    public String dashboard(Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingDate"));
        Map<Integer, String> orderItems = orders.stream().collect(Collectors.toMap(
                Order::getId,
                order -> orderItemRepository.findByOrderId(order.getId()).stream()
                        .map(item -> item.getProductNameSnapshot() + " x" + item.getQuantity())
                        .collect(Collectors.joining(", "))
        ));
        Map<Integer, String> bookingServices = bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingServiceRepository.findByBookingId(booking.getId()).stream()
                        .map(service -> service.getServiceNameSnapshot())
                        .collect(Collectors.joining(", "))
        ));

        model.addAttribute("orders", orders);
        model.addAttribute("bookings", bookings);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("bookingServices", bookingServices);
        model.addAttribute("pendingOrders", countOrderStatus(orders, "PENDING_DEPOSIT"));
        model.addAttribute("pendingBookings", countBookingStatus(bookings, "PENDING_DEPOSIT"));
        model.addAttribute("completedBookings", countBookingStatus(bookings, "COMPLETED"));
        model.addAttribute("carModels", carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc());
        model.addAttribute("carBrands", carModelRepository.findDistinctBrands());
        return "staff/dashboard";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Integer orderId,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/staff#orders";
    }

    @PostMapping("/bookings/status")
    public String updateBookingStatus(@RequestParam Integer bookingId,
                                      @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            bookingService.updateStatus(bookingId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái lịch hẹn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/staff#bookings";
    }

    @PostMapping("/car-models")
    public String createCarModel(@RequestParam String brand,
                                 @RequestParam String modelName,
                                 @RequestParam Integer year,
                                 RedirectAttributes redirectAttributes) {
        try {
            CarModel carModel = new CarModel();
            carModel.setBrand(brand.trim());
            carModel.setModelName(modelName.trim());
            carModel.setYear(year);
            carModelRepository.save(carModel);
            redirectAttributes.addFlashAttribute("successMessage", "Them dong xe catalog thanh cong!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Co loi khi them dong xe: " + e.getMessage());
        }
        return "redirect:/staff";
    }

    private long countOrderStatus(List<Order> orders, String status) {
        return orders.stream()
                .filter(order -> status.equalsIgnoreCase(order.getOrderStatus().name()))
                .count();
    }

    private long countBookingStatus(List<Booking> bookings, String status) {
        return bookings.stream()
                .filter(booking -> status.equalsIgnoreCase(booking.getBookingStatus().name()))
                .count();
    }
}
