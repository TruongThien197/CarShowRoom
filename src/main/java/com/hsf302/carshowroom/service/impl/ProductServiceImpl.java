package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.ProductStatus;
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
            return productRepository.findByNameContainingIgnoreCaseAndStatus(keyword.trim(), ProductStatus.ACTIVE);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE);
        }
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Override
    public Product getProduct(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm with ID: " + id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> findProductsPaged(Integer categoryId, String keyword, Pageable pageable) {
        return findProductsPaged(categoryId, keyword, null, null, null, null, pageable);
    }

    @Override
    public Page<Product> findProductsPaged(Integer categoryId, String keyword, Integer carModelId,
                                           String brand, String modelName, Integer year, Pageable pageable) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        String cleanKeyword = hasKeyword ? keyword.trim() : "";
        return productRepository.searchCatalog(
                categoryId,
                hasKeyword ? cleanKeyword : null,
                carModelId,
                normalize(brand),
                normalize(modelName),
                year,
                pageable
        );
    }

    @Override
    public Page<Product> findAdminProductsPaged(Integer categoryId, String keyword, Pageable pageable) {
        return findAdminProductsPaged(categoryId, keyword, null, null, null, null, pageable);
    }

    @Override
    public Page<Product> findAdminProductsPaged(Integer categoryId, String keyword, Integer carModelId,
                                                String brand, String modelName, Integer year, Pageable pageable) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        String cleanKeyword = hasKeyword ? keyword.trim() : "";
        return productRepository.searchAdminCatalog(
                categoryId,
                hasKeyword ? cleanKeyword : null,
                carModelId,
                normalize(brand),
                normalize(modelName),
                year,
                pageable
        );
    }

    @Override
    public Product createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Integer id, Product updatedProduct) {
        Product existing = getProduct(id);
        existing.setName(updatedProduct.getName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setPrice(updatedProduct.getPrice());
        existing.setPhysicalStock(updatedProduct.getPhysicalStock());
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
        existing.setStatus(ProductStatus.INACTIVE);
        productRepository.save(existing);
    }

    @Override
    public Product changeStatus(Integer id, String status) {
        Product existing = getProduct(id);
        existing.setStatus(ProductStatus.valueOf(status.toUpperCase()));
        return productRepository.save(existing);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
