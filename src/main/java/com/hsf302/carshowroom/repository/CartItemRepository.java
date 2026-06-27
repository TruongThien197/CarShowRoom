package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
}
