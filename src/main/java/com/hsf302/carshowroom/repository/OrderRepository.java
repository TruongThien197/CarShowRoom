package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
