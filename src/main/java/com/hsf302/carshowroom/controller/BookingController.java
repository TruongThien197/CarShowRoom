package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.dto.PayOS.PayOSCreatePaymentLinkRequest;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.BookingService;
import com.hsf302.carshowroom.service.PaymentService;
import com.hsf302.carshowroom.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {
    private final AuthService authService;
    private final BookingService bookingService;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final PaymentService paymentService;
    private final SchedulingService schedulingService;

    @GetMapping
    public String form(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        populateModel(model, user, new BookingForm());
        return "booking/create";
    }

    @GetMapping("/create")
    public String createFormAlias(Model model) {
        return form(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            populateModel(model, user, form);
            return "booking/create";
        }
        Booking booking;
        try {
            booking = bookingService.createBooking(user, form);
        } catch (RuntimeException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            populateModel(model, user, form);
            return "booking/create";
        }
        try {
            PaymentTransaction transaction = createBookingPayment(user, booking);
            return "redirect:" + transaction.getCheckoutUrl();
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage",
                    "Lịch hẹn đã được lưu nhưng chưa tạo được liên kết PayOS: " + exception.getMessage());
            return "redirect:/booking/" + booking.getId() + "/payment";
        }
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        List<Booking> bookings = bookingService.getBookings(user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", buildBookingServices(bookings));
        return "booking/my-bookings";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        Booking booking = bookingService.getBookingDetail(user, id);
        model.addAttribute("booking", booking);
        model.addAttribute("bookingServices", bookingServiceRepository.findByBookingId(booking.getId()));
        return "booking/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        bookingService.cancelBooking(user, id);
        return "redirect:/booking/my-bookings";
    }

    @PostMapping("/{id}/refund-account")
    public String submitRefundAccount(@PathVariable Integer id,
                                      @RequestParam String bankName,
                                      @RequestParam String bankBin,
                                      @RequestParam String accountHolder,
                                      @RequestParam String accountNumber,
                                      RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            bookingService.submitRefundAccount(user, id, bankName, bankBin, accountHolder, accountNumber);
            attributes.addFlashAttribute("successMessage", "Đã lưu thông tin tài khoản nhận tiền hoàn.");
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/booking/" + id;
    }

    @GetMapping("/available-slots")
    @ResponseBody
    public ResponseEntity<?> availableSlots(@RequestParam LocalDate date,
                                            @RequestParam Integer serviceId) {
        try {
            return ResponseEntity.ok(schedulingService.findAvailableSlots(date, List.of(serviceId)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/{id}/payment")
    public String payment(@PathVariable Integer id, Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        Booking booking = bookingService.getBookingDetail(user, id);
        if (booking.getPaymentStatus() == com.hsf302.carshowroom.common.Enums.PaymentStatus.PAID) {
            return "redirect:/booking/" + id;
        }
        model.addAttribute("booking", booking);
        return "booking/payment";
    }

    @PostMapping("/{id}/payment")
    public String retryPayment(@PathVariable Integer id, RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            Booking booking = bookingService.getBookingDetail(user, id);
            PaymentTransaction transaction = createBookingPayment(user, booking);
            return "redirect:" + transaction.getCheckoutUrl();
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/booking/" + id + "/payment";
        }
    }

    @PostMapping("/{id}/remaining-payment")
    public String remainingPayment(@PathVariable Integer id, RedirectAttributes attributes) {
        User user = currentUserOrNull();
        if (user == null) return "redirect:/auth/login";
        try {
            Booking booking = bookingService.getBookingDetail(user, id);
            BigDecimal finalAmount = booking.getFinalAmount();
            BigDecimal deposit = booking.getDepositAmount() == null ? BigDecimal.ZERO : booking.getDepositAmount();
            if (booking.getBookingStatus() != BookingStatus.IN_PROGRESS
                    && booking.getBookingStatus() != BookingStatus.COMPLETED) {
                throw new IllegalStateException("Chỉ có thể thanh toán phần còn lại sau khi xe đã được tiếp nhận.");
            }
            if (finalAmount == null || finalAmount.compareTo(deposit) <= 0) {
                throw new IllegalStateException("Nhân viên chưa nhập giá cuối hoặc lịch này không còn khoản phải thanh toán.");
            }
            if (booking.getRemainingPaymentStatus() == PaymentStatus.PAID) {
                throw new IllegalStateException("Khoản thanh toán còn lại đã được thanh toán.");
            }
            PaymentTransaction transaction = paymentService.createPaymentLink(PayOSCreatePaymentLinkRequest.builder()
                    .user(user).booking(booking).paymentPurpose("REMAINING").subOrders(List.of()).build());
            return "redirect:" + transaction.getCheckoutUrl();
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/booking/" + id;
        }
    }

    private void populateModel(Model model, User user, BookingForm form) {
        List<Booking> bookings = bookingService.getBookings(user);
        model.addAttribute("bookingForm", form);
        model.addAttribute("services", serviceRepository.findByStatusOrderByServiceNameAsc(ServiceStatus.ACTIVE));
        model.addAttribute("vehicles", vehicleRepository.findByUser(user));
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", buildBookingServices(bookings));
    }

    private Map<Integer, String> buildBookingServices(List<Booking> bookings) {
        return bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingServiceRepository.findByBookingId(booking.getId()).stream()
                        .map(service -> service.getService() != null
                                ? service.getService().getServiceName()
                                : service.getServiceNameSnapshot())
                        .collect(Collectors.joining(", "))
        ));
    }

    private PaymentTransaction createBookingPayment(User user, Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.EXPIRED_PAYMENT) {
            bookingService.reopenDepositPayment(booking.getId());
            booking = bookingService.getBookingDetail(user, booking.getId());
        }
        if (booking.getPaymentStatus() == com.hsf302.carshowroom.common.Enums.PaymentStatus.PAID) {
            throw new IllegalStateException("Lịch hẹn này đã được thanh toán.");
        }
        if (booking.getBookingStatus() != com.hsf302.carshowroom.common.Enums.BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Lịch hẹn không còn ở trạng thái chờ thanh toán.");
        }
        return paymentService.createPaymentLink(PayOSCreatePaymentLinkRequest.builder()
                .user(user)
                .booking(booking)
                .subOrders(List.of())
                .build());
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
