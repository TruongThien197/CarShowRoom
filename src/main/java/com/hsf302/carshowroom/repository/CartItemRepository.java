package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProductAndFulfillmentType(User user, Product product, FulfillmentType fulfillmentType);

    void deleteByUser(User user);
}
