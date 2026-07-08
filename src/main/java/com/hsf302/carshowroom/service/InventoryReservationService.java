package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.Product;

import java.util.List;

public interface InventoryReservationService {
    void checkStockAvailability(Product product, int quantity);
    void reserveStock(List<Order> orders);
    void confirmReservation(Order order);
    void releaseReservation(Order order);
    void consumeStock(Order order);
}
