package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.BookingService;
import com.hsf302.carshowroom.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final AuthService authService;
    private final OrderService orderService;
    private final BookingService bookingService;
    private final VehicleRepository vehicleRepository;

    @GetMapping
    public String profile(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("ordersCount", orderService.getOrders(user).size());
        model.addAttribute("bookingsCount", bookingService.getBookings(user).size());
        model.addAttribute("vehiclesCount", vehicleRepository.findByUser(user).size());
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                RedirectAttributes attributes) {
        try {
            User user = authService.getCurrentUser();
            authService.updateProfile(user.getId(), fullName, phone, address);
            attributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ.");
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes attributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
            }
            User user = authService.getCurrentUser();
            authService.changePassword(user.getId(), currentPassword, newPassword);
            attributes.addFlashAttribute("successMessage", "Đã đổi mật khẩu.");
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/account";
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
