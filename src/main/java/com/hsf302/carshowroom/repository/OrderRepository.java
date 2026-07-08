package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.common.Enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    List<Order> findByOrderStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findAllByOrderByIdAsc();

    List<Order> findAllByOrderByCreatedAtAsc();
}
