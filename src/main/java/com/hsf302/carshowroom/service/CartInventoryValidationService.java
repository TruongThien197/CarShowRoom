package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;

import java.util.List;

public interface CartInventoryValidationService {
    void validateCartItemStock(User user, Product product, Integer currentCartItemId, int additionalQuantity);
    void validateCheckoutStock(List<CartItem> cartItems);
}
