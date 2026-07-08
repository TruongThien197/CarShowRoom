package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CheckoutRequestDTO;
import com.hsf302.carshowroom.dto.CheckoutResponseDTO;
import com.hsf302.carshowroom.entity.User;

public interface CheckoutService {
    CheckoutResponseDTO processCheckout(User user, CheckoutRequestDTO checkoutRequest);
}
