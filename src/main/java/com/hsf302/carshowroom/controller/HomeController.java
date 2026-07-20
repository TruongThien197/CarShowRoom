package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.findProducts(null, null).stream().limit(4).toList());
        return "home";
    }

    @GetMapping("/checkout")
    public String checkoutRedirect() {
        return "redirect:/orders/checkout";
    }

}
