package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.dto.CheckoutResult;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.exception.InsufficientStockException;
import com.hsf302.carshowroom.exception.MixedFulfillmentException;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.ShippingFeeRuleRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.CartService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.RefundService;
import com.hsf302.carshowroom.service.SystemSettingService;
import com.hsf302.carshowroom.service.impl.SystemSettingServiceImpl;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final String WORKSHOP_INSTALLATION_SERVICE = "Thay thế phụ tùng";

    private final AuthService authService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final RefundService refundService;
    private final SystemSettingService settingService;


    @GetMapping("/checkout")
    public String checkout(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        List<CartItem> cartItems = cartService.getCartItems(user);
        CheckoutForm form = new CheckoutForm();
        form.setShippingAddress(user.getAddress());
        form.setPhone(user.getPhone());
        populateCheckoutModel(model, user, cartItems, form);
        return "order/checkout";
    }

    /**
     * Nhận thông tin checkout, khóa dịch vụ lắp đặt mặc định cho đơn tại xưởng
     * và chuyển khách tới PayOS khi cần thanh toán trực tuyến.
     */
    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        List<CartItem> cartItems = cartService.getCartItems(user);
        applyDefaultWorkshopService(cartItems, form);
        if (bindingResult.hasErrors()) {
            populateCheckoutModel(model, user, cartItems, form);
            return "order/checkout";
        }
        try {
            CheckoutResult result = orderService.checkout(user, form);
            if (result.requiresOnlinePayment()) {
                return "redirect:" + result.checkoutUrl();
            }
            return "redirect:/orders/" + result.orderId();
        } catch (InsufficientStockException | MixedFulfillmentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/cart";
        } catch (RuntimeException exception) {
            model.addAttribute("errorMessage", "Không thể tạo thanh toán PayOS: " + exception.getMessage());
            populateCheckoutModel(model, user, cartItems, form);
            return "order/checkout";
        }
    }

    @GetMapping
    public String history(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        List<Order> orders = orderService.getOrders(user);
        Map<Integer, String> orderItems = orders.stream().collect(Collectors.toMap(
                Order::getId,
                order -> orderItemRepository.findByOrderId(order.getId()).stream()
                        .map(item -> item.getProductNameSnapshot() + " x" + item.getQuantity())
                        .collect(Collectors.joining(", "))
        ));

        model.addAttribute("orders", orders);
        model.addAttribute("orderItems", orderItems);
        return "order/history";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        Order order = orderService.getOrderForUser(id, user);
        BigDecimal totalAmount = orderService.calculateTotalAmount(order);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItemRepository.findByOrderId(id));
        model.addAttribute("refundTransactions", refundService.getOrderRefunds(order));
        model.addAttribute("canCancel", order.getOrderStatus() != OrderStatus.SHIPPING
                && order.getOrderStatus() != OrderStatus.COMPLETED
                && order.getOrderStatus() != OrderStatus.CANCELED
                && order.getOrderStatus() != OrderStatus.EXPIRED_PAYMENT);
        model.addAttribute("canEditAddress", order.getOrderType() == com.hsf302.carshowroom.common.Enums.OrderType.SHIPPING
                && (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT || order.getOrderStatus() == OrderStatus.PROCESSING));
        model.addAttribute("canRetryPayment", canRetryPayment(order));
        model.addAttribute("totalAmount", totalAmount);
        return "order/detail";
    }

    @GetMapping("/{id}/payment")
    public String paymentConfirmation(@PathVariable Integer id, Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        Order order = orderService.getOrderForUser(id, user);
        if (!canRetryPayment(order)) {
            return "redirect:/orders/" + id;
        }
        model.addAttribute("order", order);
        return "order/payment-confirmation";
    }

    @PostMapping("/{id}/payment")
    public String choosePaymentMethod(@PathVariable Integer id,
                                      @RequestParam String paymentMethod,
                                      RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            CheckoutResult result = orderService.choosePaymentMethod(id, user, paymentMethod);
            if (result.requiresOnlinePayment()) {
                return "redirect:" + result.checkoutUrl();
            }
            attributes.addFlashAttribute("successMessage", "Đơn hàng đã được xác nhận thanh toán khi nhận hàng.");
            return "redirect:/orders/" + result.orderId();
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/orders/" + id + "/payment";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id, @RequestParam String reason, RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.cancelOrderForUser(id, user, reason);
            attributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu hủy đơn. Nhân viên sẽ duyệt trước khi hủy đơn và xử lý hoàn tiền.");
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/shipping-address")
    public String updateShippingAddress(@PathVariable Integer id,
                                        @RequestParam String shippingAddress,
                                        @RequestParam String receiverPhone,
                                        RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.updateShippingAddressForUser(id, user, shippingAddress, receiverPhone);
            attributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin nhận hàng.");
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    /** Chuẩn bị dữ liệu hiển thị cho checkout, gồm tiền cọc và khoảng chi phí lắp đặt. */
    private void populateCheckoutModel(Model model, User user, List<CartItem> cartItems, CheckoutForm form) {

        model.addAttribute("user", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", cartService.calculateSubtotal(cartItems));
        model.addAttribute("totalAmount", cartService.calculateTotalAmount(cartItems, BigDecimal.ZERO));

        model.addAttribute("checkoutForm", form);
        boolean needsWorkshop = cartItems.stream()
                .anyMatch(item -> FulfillmentType.AT_WORKSHOP.equals(item.getFulfillmentType()));
        boolean hasShipping = cartItems.stream()
                .anyMatch(item -> !FulfillmentType.AT_WORKSHOP.equals(item.getFulfillmentType()));
        model.addAttribute("needsWorkshop", needsWorkshop);
        model.addAttribute("hasShipping", hasShipping);
        model.addAttribute("vehicles", vehicleRepository.findByUser(user));
        if (needsWorkshop) {
            com.hsf302.carshowroom.entity.Service workshopService = getWorkshopInstallationService();
            form.setServiceId(workshopService.getId());
            form.setPaymentMethod("PAYOS");
            BigDecimal deposit = calculateDeposit(workshopService.getMinPrice());
            BigDecimal productTotal = cartService.calculateSubtotal(cartItems);
            model.addAttribute("workshopService", workshopService);
            model.addAttribute("workshopDeposit", deposit);
            model.addAttribute("depositRatePercent",
                    settingService.getInt(SystemSettingServiceImpl.DEPOSIT_RATE_PERCENT));
            model.addAttribute("minDepositAmount",
                    settingService.getInt(SystemSettingServiceImpl.MIN_DEPOSIT_AMOUNT));
            model.addAttribute("maxDepositAmount",
                    settingService.getInt(SystemSettingServiceImpl.MAX_DEPOSIT_AMOUNT));
            model.addAttribute("minBookingLeadMinutes",
                    settingService.getInt(SystemSettingServiceImpl.MIN_BOOKING_LEAD_MINUTES));
            model.addAttribute("workshopTotalMin", productTotal.add(workshopService.getMinPrice()));
            model.addAttribute("workshopTotalMax", productTotal.add(workshopService.getMaxPrice()));
            model.addAttribute("workshopRemainingMin", productTotal.add(workshopService.getMinPrice()).subtract(deposit));
            model.addAttribute("workshopRemainingMax", productTotal.add(workshopService.getMaxPrice()).subtract(deposit));
        }
    }

    /** Không cho khách thay đổi dịch vụ: đơn lắp đặt luôn dùng "Thay thế phụ tùng". */
    private void applyDefaultWorkshopService(List<CartItem> cartItems, CheckoutForm form) {
        boolean needsWorkshop = cartItems.stream()
                .anyMatch(item -> FulfillmentType.AT_WORKSHOP.equals(item.getFulfillmentType()));
        if (needsWorkshop) {
            form.setServiceId(getWorkshopInstallationService().getId());
            form.setPaymentMethod("PAYOS");
        }
    }

    /** Lấy dịch vụ cố định đang hoạt động; báo lỗi rõ ràng khi quản trị viên đã tắt dịch vụ. */
    private com.hsf302.carshowroom.entity.Service getWorkshopInstallationService() {
        return serviceRepository.findFirstByServiceNameIgnoreCase(WORKSHOP_INSTALLATION_SERVICE)
                .filter(service -> service.getStatus() == com.hsf302.carshowroom.common.Enums.ServiceStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Dịch vụ mặc định 'Thay thế phụ tùng' chưa sẵn sàng. Vui lòng kích hoạt dịch vụ này trong trang quản trị."));
    }

    /** Tính tiền cọc theo tỷ lệ và mức trần/sàn do quản trị viên cấu hình. */
    private BigDecimal calculateDeposit(BigDecimal serviceFee) {
        return serviceFee.multiply(BigDecimal.valueOf(
                        settingService.getInt(SystemSettingServiceImpl.DEPOSIT_RATE_PERCENT)))
                .movePointLeft(2)
                .max(BigDecimal.valueOf(settingService.getInt(SystemSettingServiceImpl.MIN_DEPOSIT_AMOUNT)))
                .min(BigDecimal.valueOf(settingService.getInt(SystemSettingServiceImpl.MAX_DEPOSIT_AMOUNT)))
                .setScale(0, RoundingMode.UP);
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean canRetryPayment(Order order) {
        return order.getPaymentStatus() != PaymentStatus.PAID
                && (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT
                || order.getOrderStatus() == OrderStatus.EXPIRED_PAYMENT);
    }
}
