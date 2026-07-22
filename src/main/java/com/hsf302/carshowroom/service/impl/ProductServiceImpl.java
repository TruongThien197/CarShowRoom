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
        validateProduct(product, null);
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Integer id, Product updatedProduct) {
        Product existing = getProduct(id);
        validateProduct(updatedProduct, id);
        existing.setName(updatedProduct.getName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setSku(updatedProduct.getSku());
        existing.setPrice(updatedProduct.getPrice());
        existing.setPhysicalStock(updatedProduct.getPhysicalStock());
        existing.setDescription(updatedProduct.getDescription());
        existing.getCompatibleCarModels().clear();
        existing.getCompatibleCarModels().addAll(updatedProduct.getCompatibleCarModels());
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

    private void validateProduct(Product product, Integer productId) {
        if (product == null || !StringUtils.hasText(product.getName())) {
            throw new IllegalArgumentException("Product name must not be empty.");
        }
        if (product.getName().trim().length() > 150) {
            throw new IllegalArgumentException("Product name must not exceed 150 characters.");
        }
        if (!StringUtils.hasText(product.getSku())) {
            throw new IllegalArgumentException("SKU must not be empty.");
        }
        String normalizedSku = product.getSku().trim();
        boolean skuUsed = productId == null
                ? productRepository.existsBySkuIgnoreCase(normalizedSku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(normalizedSku, productId);
        if (skuUsed) {
            throw new IllegalArgumentException("SKU already belongs to another product.");
        }
        if (product.getCategory() == null) {
            throw new IllegalArgumentException("Product category must not be empty.");
        }
        if (product.getPrice() == null || product.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Product price must not be negative.");
        }
        if (product.getPhysicalStock() == null || product.getPhysicalStock() < 0) {
            throw new IllegalArgumentException("Stock quantity must not be negative.");
        }
        if (productId != null) {
            Product existing = productRepository.findById(productId).orElseThrow();
            int reservedStock = existing.getReservedStock() == null ? 0 : existing.getReservedStock();
            if (product.getPhysicalStock() < reservedStock) {
                throw new IllegalArgumentException(
                        "Stock quantity cannot be lower than the currently reserved stock."
                );
            }
        }
        product.setName(product.getName().trim());
        product.setSku(normalizedSku);
    }
}
