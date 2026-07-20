package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.dto.CheckoutResult;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.User;

import java.util.List;

public interface OrderService {
    CheckoutResult checkout(User user, CheckoutForm form);

    List<Order> getOrders(User user);

    Order getOrderById(Integer id);

    void updateOrderStatus(Integer id, String status);

    Order getOrderForUser(Integer id, User user);

    void cancelOrderForUser(Integer id, User user, String reason);

    void updateShippingAddressForUser(Integer id, User user, String shippingAddress, String receiverPhone);

    void updateShipment(Integer id, String shippingCarrier, String trackingCode);

    CheckoutResult choosePaymentMethod(Integer id, User user, String paymentMethod);
}
