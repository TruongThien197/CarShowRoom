package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    List<CartItem> getCartItems(User user);

    void addToCart(User user, Integer productId, Integer quantity);

//    void updateQuantity(User user, Integer cartItemId, Integer quantity);

    void updateQuantity(User user, Integer cartItemId, Integer quantity);

    void removeItem(User user, Integer cartItemId);

    BigDecimal calculateSubtotal(List<CartItem> items);

    void clearCart(User user);
}
