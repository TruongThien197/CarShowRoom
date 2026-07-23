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

    /** Tìm sản phẩm đang bán theo từ khóa hoặc danh mục cho giao diện cửa hàng. */
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

    /** Lấy chi tiết sản phẩm theo mã, báo lỗi nếu không tồn tại. */
    @Override
    public Product getProduct(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm with ID: " + id));
    }

    /** Lấy toàn bộ sản phẩm, bao gồm cả sản phẩm đã ngừng bán, cho quản trị. */
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /** Tìm kiếm và phân trang sản phẩm công khai với bộ lọc cơ bản. */
    @Override
    public Page<Product> findProductsPaged(Integer categoryId, String keyword, Pageable pageable) {
        return findProductsPaged(categoryId, keyword, null, null, null, null, pageable);
    }

    /** Tìm kiếm catalog công khai theo danh mục, từ khóa và khả năng tương thích dòng xe. */
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

    /** Tìm kiếm và phân trang sản phẩm cho quản trị với bộ lọc cơ bản. */
    @Override
    public Page<Product> findAdminProductsPaged(Integer categoryId, String keyword, Pageable pageable) {
        return findAdminProductsPaged(categoryId, keyword, null, null, null, null, pageable);
    }

    /** Tìm kiếm catalog quản trị, bao gồm cả sản phẩm không còn bán. */
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

    /** Tạo sản phẩm mới và đặt trạng thái hoạt động nếu chưa được chỉ định. */
    @Override
    public Product createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return productRepository.save(product);
    }

    /** Cập nhật các thông tin có thể chỉnh sửa của sản phẩm. */
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

    /** Ngừng bán sản phẩm bằng cách chuyển sang trạng thái không hoạt động. */
    @Override
    public void deleteProduct(Integer id) {
        Product existing = getProduct(id);
        existing.setStatus(ProductStatus.INACTIVE);
        productRepository.save(existing);
    }

    /** Thay đổi trực tiếp trạng thái hoạt động của sản phẩm. */
    @Override
    public Product changeStatus(Integer id, String status) {
        Product existing = getProduct(id);
        existing.setStatus(ProductStatus.valueOf(status.toUpperCase()));
        return productRepository.save(existing);
    }

    /** Chuẩn hóa giá trị lọc: bỏ khoảng trắng và đổi giá trị rỗng thành null. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
