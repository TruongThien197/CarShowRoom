package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;

import java.util.List;

public interface OrderService {
    Order checkout(User user, CheckoutForm form);

    List<Order> getOrders(User user);

    List<Order> getAllOrders();

    Order getOrderById(Integer id);

    void updateOrderStatus(Integer id, String status);
}
