package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.OrderDetailRepository;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.CartService;
import com.hsf302.carshowroom.service.OrderService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final AuthService authService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderDetailRepository orderDetailRepository;

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

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,
                             BindingResult bindingResult,
                             Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        List<CartItem> cartItems = cartService.getCartItems(user);
        if (bindingResult.hasErrors()) {
            populateCheckoutModel(model, user, cartItems, form);
            return "order/checkout";
        }
        orderService.checkout(user, form);
        return "redirect:/orders";
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
                order -> orderDetailRepository.findByOrderId(order.getId()).stream()
                        .map(detail -> detail.getProduct().getProductName() + " x" + detail.getQuantity())
                        .collect(Collectors.joining(", "))
        ));
        model.addAttribute("orders", orders);
        model.addAttribute("orderItems", orderItems);
        return "order/history";
    }

    private void populateCheckoutModel(Model model, User user, List<CartItem> cartItems, CheckoutForm form) {
        model.addAttribute("user", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", cartService.calculateSubtotal(cartItems));
        model.addAttribute("checkoutForm", form);
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
