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

    /** Chuyển đơn sang đang xử lý và xác nhận phần hàng đã được giữ. */
    @Override
    @Transactional
    public void processOrder(Order order) {
        order.setOrderStatus(OrderStatus.PROCESSING);
        inventoryReservationService.confirmReservation(order);
        orderRepository.save(order);
    }

    /** Chuyển đơn giao hàng sang trạng thái đang vận chuyển. */
    @Override
    @Transactional
    public void shipOrder(Order order) {
        order.setOrderStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);
    }

    /** Hoàn tất đơn, trừ tồn kho thực tế và cập nhật trạng thái đơn. */
    @Override
    @Transactional
    public void completeOrder(Order order) {
        inventoryReservationService.consumeStock(order);
        order.setOrderStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    /** Hủy đơn và trả lại hàng đang giữ nếu đơn chưa hoàn tất. */
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
