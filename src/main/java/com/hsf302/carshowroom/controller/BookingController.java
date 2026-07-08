package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
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
    private final BookingServiceRepository bookingServiceRepository;

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
                         Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            populateModel(model, user, form);
            return "booking/create";
        }
        bookingService.createBooking(user, form);
        return "redirect:/booking/my-bookings";
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

    private void populateModel(Model model, User user, BookingForm form) {
        List<Booking> bookings = bookingService.getBookings(user);
        model.addAttribute("bookingForm", form);
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("vehicles", vehicleRepository.findByUser(user));
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", buildBookingServices(bookings));
    }

    private Map<Integer, String> buildBookingServices(List<Booking> bookings) {
        return bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingServiceRepository.findByBookingId(booking.getId()).stream()
                        .map(service -> service.getServiceNameSnapshot())
                        .collect(Collectors.joining(", "))
        ));
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
