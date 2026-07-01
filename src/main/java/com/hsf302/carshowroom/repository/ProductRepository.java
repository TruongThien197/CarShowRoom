package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByStatusIgnoreCase(String status);

    List<Product> findByCategoryIdAndStatusIgnoreCase(Integer categoryId, String status);

    List<Product> findByProductNameContainingIgnoreCaseAndStatusIgnoreCase(String keyword, String status);
}
