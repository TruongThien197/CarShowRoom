package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.BookingDetailRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    private final BookingDetailRepository bookingDetailRepository;

    @GetMapping
    public String form(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        populateModel(model, user, new BookingForm());
        return "booking/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            populateModel(model, user, form);
            return "booking/form";
        }
        bookingService.createBooking(user, form);
        return "redirect:/booking";
    }

    private void populateModel(Model model, User user, BookingForm form) {
        List<Booking> bookings = bookingService.getBookings(user);
        Map<Integer, String> bookingServices = bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingDetailRepository.findByBookingId(booking.getId()).stream()
                        .map(detail -> detail.getService().getServiceName())
                        .collect(Collectors.joining(", "))
        ));
        model.addAttribute("bookingForm", form);
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("vehicles", vehicleRepository.findByUser(user));
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", bookingServices);
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
