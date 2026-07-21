package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.service.InventoryReservationService;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderWorkflowServiceImpl implements OrderWorkflowService {
    private final OrderRepository orderRepository;
    private final InventoryReservationService inventoryReservationService;

    @Override
    @Transactional
    public void processOrder(Order order) {
        order.setOrderStatus(OrderStatus.PROCESSING);
        inventoryReservationService.confirmReservation(order);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void shipOrder(Order order) {
        order.setOrderStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void completeOrder(Order order) {
        inventoryReservationService.consumeStock(order);
        order.setOrderStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Order order) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            inventoryReservationService.releaseReservation(order);
        }
        order.setOrderStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
    }
}
