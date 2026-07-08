package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Integer orderId);
}
