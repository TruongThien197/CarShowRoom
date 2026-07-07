package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Order;

public interface OrderWorkflowService {
    void processOrder(Order order);
    void shipOrder(Order order);
    void completeOrder(Order order);
    void cancelOrder(Order order);
}
