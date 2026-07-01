package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.repository.BookingDetailRepository;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.OrderDetailRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;

    @GetMapping
    public String dashboard(Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
        List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingDate"));
        Map<Integer, String> orderItems = orders.stream().collect(Collectors.toMap(
                Order::getId,
                order -> orderDetailRepository.findByOrderId(order.getId()).stream()
                        .map(detail -> detail.getProduct().getProductName() + " x" + detail.getQuantity())
                        .collect(Collectors.joining(", "))
        ));
        Map<Integer, String> bookingServices = bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingDetailRepository.findByBookingId(booking.getId()).stream()
                        .map(detail -> detail.getService().getServiceName())
                        .collect(Collectors.joining(", "))
        ));

        model.addAttribute("orders", orders);
        model.addAttribute("bookings", bookings);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("bookingServices", bookingServices);
        model.addAttribute("pendingOrders", countStatus(orders, "PENDING"));
        model.addAttribute("pendingBookings", countStatus(bookings, "PENDING"));
        model.addAttribute("completedBookings", countStatus(bookings, "COMPLETED"));
        return "staff/dashboard";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Integer orderId, @RequestParam String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
        return "redirect:/staff#orders";
    }

    @PostMapping("/bookings/status")
    public String updateBookingStatus(@RequestParam Integer bookingId, @RequestParam String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(status);
        bookingRepository.save(booking);
        return "redirect:/staff#bookings";
    }

    private long countStatus(List<? extends Object> items, String status) {
        return items.stream().filter(item -> {
            if (item instanceof Order order) {
                return status.equalsIgnoreCase(order.getStatus());
            }
            if (item instanceof Booking booking) {
                return status.equalsIgnoreCase(booking.getStatus());
            }
            return false;
        }).count();
    }
}
