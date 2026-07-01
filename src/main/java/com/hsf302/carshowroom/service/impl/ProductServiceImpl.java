package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public List<Product> findProducts(Integer categoryId, String keyword) {
        if (StringUtils.hasText(keyword)) {
            return productRepository.findByProductNameContainingIgnoreCaseAndStatusIgnoreCase(keyword.trim(), "ACTIVE");
        }
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndStatusIgnoreCase(categoryId, "ACTIVE");
        }
        return productRepository.findByStatusIgnoreCase("ACTIVE");
    }

    @Override
    public Product getProduct(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}
