package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> findProductsPaged(Integer categoryId, String keyword, Pageable pageable) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        String cleanKeyword = hasKeyword ? keyword.trim() : "";

        if (categoryId != null && hasKeyword) {
            return productRepository.findByCategoryIdAndProductNameContainingIgnoreCaseAndStatusIgnoreCase(categoryId, cleanKeyword, "ACTIVE", pageable);
        }
        if (hasKeyword) {
            return productRepository.findByProductNameContainingIgnoreCaseAndStatusIgnoreCase(cleanKeyword, "ACTIVE", pageable);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndStatusIgnoreCase(categoryId, "ACTIVE", pageable);
        }
        return productRepository.findByStatusIgnoreCase("ACTIVE", pageable);
    }

    @Override
    public Page<Product> findAdminProductsPaged(Integer categoryId, String keyword, Pageable pageable) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        String cleanKeyword = hasKeyword ? keyword.trim() : "";

        if (categoryId != null && hasKeyword) {
            return productRepository.findByCategoryIdAndProductNameContainingIgnoreCase(categoryId, cleanKeyword, pageable);
        }
        if (hasKeyword) {
            return productRepository.findByProductNameContainingIgnoreCase(cleanKeyword, pageable);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable);
        }
        return productRepository.findAll(pageable);
    }

    @Override
    public Product createProduct(Product product) {
        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus("ACTIVE");
        }
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Integer id, Product updatedProduct) {
        Product existing = getProduct(id);
        existing.setProductName(updatedProduct.getProductName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStockQuantity(updatedProduct.getStockQuantity());
        existing.setDescription(updatedProduct.getDescription());
        if (updatedProduct.getImageUrl() != null) {
            existing.setImageUrl(updatedProduct.getImageUrl());
        }
        if (updatedProduct.getStatus() != null) {
            existing.setStatus(updatedProduct.getStatus());
        }
        return productRepository.save(existing);
    }

    @Override
    public void deleteProduct(Integer id) {
        Product existing = getProduct(id);
        existing.setStatus("INACTIVE"); // Soft delete
        productRepository.save(existing);
    }

    @Override
    public Product changeStatus(Integer id, String status) {
        Product existing = getProduct(id);
        existing.setStatus(status);
        return productRepository.save(existing);
    }
}