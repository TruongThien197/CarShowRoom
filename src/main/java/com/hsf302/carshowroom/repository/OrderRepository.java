package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findByStatusOrderByOrderDateAsc(String status);

    List<Order> findAllByOrderByIdAsc();

    List<Order> findAllByOrderByOrderDateAsc();
}
