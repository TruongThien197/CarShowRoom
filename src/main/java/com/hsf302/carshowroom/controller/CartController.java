package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final AuthService authService;
    private final CartService cartService;

    @GetMapping
    public String cart(Model model, RedirectAttributes redirectAttributes) {
        User user = currentUserOrNull();
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to view your cart.");
            return "redirect:/auth/login";
        }
        List<CartItem> cartItems = cartService.getCartItems(user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", cartService.calculateSubtotal(cartItems));
        return "cart/index";
    }

    @PostMapping("/add")
    public String add(@RequestParam Integer productId,
                      @RequestParam(defaultValue = "1") Integer quantity,
                      @RequestParam(defaultValue = "SHIPPING") String fulfillmentType,
                      RedirectAttributes redirectAttributes) {
        User user = currentUserOrNull();
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to add items to your cart.");
            return "redirect:/auth/login";
        }
        try{
            cartService.addToCart(user, productId, quantity, fulfillmentType);
            redirectAttributes.addFlashAttribute("successMessage", "Product added to cart successfully.");
        }catch (RuntimeException ex){
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam Integer cartItemId,
                         @RequestParam Integer quantity,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserOrNull();
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to update your cart.");
            return "redirect:/auth/login";
        }
        cartService.updateQuantity(user, cartItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Integer cartItemId,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserOrNull();
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to remove items from your cart.");
            return "redirect:/auth/login";
        }
        cartService.removeItem(user, cartItemId);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product removed from cart successfully."
        );
        return "redirect:/cart";
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
