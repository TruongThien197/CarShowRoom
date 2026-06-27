package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
}
