package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
