package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CartDTO;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.User;
import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    CartDTO getCart(User user);
    CartDTO addItemToCart(User user, Long productId, int quantity, String fulfillmentType);
    CartDTO updateCartItem(User user, Long itemId, int quantity);
    CartDTO removeCartItem(User user, Long itemId);
    void clearCart(User user);

    // Additional methods for compatibility
    List<CartItem> getCartItems(User user);
    BigDecimal calculateSubtotal(List<CartItem> items);
    void addToCart(User user, Integer productId, Integer quantity);
    void addToCart(User user, Integer productId, Integer quantity, String fulfillmentType);
    void updateQuantity(User user, Integer itemId, Integer quantity);
    void removeItem(User user, Integer itemId);
}
