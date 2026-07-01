package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    List<Product> findProducts(Integer categoryId, String keyword);

    Product getProduct(Integer id);

    List<Product> getAllProducts();

    Page<Product> findProductsPaged(Integer categoryId, String keyword, Pageable pageable);

    Page<Product> findAdminProductsPaged(Integer categoryId, String keyword, Pageable pageable);

    Product createProduct(Product product);

    Product updateProduct(Integer id, Product updatedProduct);

    void deleteProduct(Integer id);

    Product changeStatus(Integer id, String status);
}