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

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
