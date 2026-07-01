package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> findProducts(Integer categoryId, String keyword);

    Product getProduct(Integer id);
}
