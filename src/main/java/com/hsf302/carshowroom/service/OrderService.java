package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.User;

import java.util.List;

public interface OrderService {
    PaymentTransaction checkout(User user, CheckoutForm form);

    List<Order> getOrders(User user);

    Order getOrderById(Integer id);

    void updateOrderStatus(Integer id, String status);
}
