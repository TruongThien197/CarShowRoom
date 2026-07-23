package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.repository.CategoryRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /** Lấy toàn bộ danh mục để hiển thị cho trang cửa hàng và quản trị. */
    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /** Tìm một danh mục theo mã; báo lỗi khi danh mục không tồn tại. */
    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục with ID: " + id));
    }

    /** Tạo và lưu danh mục mới. */
    @Override
    public Category createCategory(Category category) {
        validateCategory(category);
        return categoryRepository.save(category);
    }

    /** Cập nhật tên và mô tả của danh mục đã tồn tại. */
    @Override
    public Category updateCategory(Integer id, Category updatedCategory) {
        Category existing = getCategoryById(id);
        validateCategory(updatedCategory);
        existing.setCategoryName(updatedCategory.getCategoryName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

    /** Xóa danh mục sau khi xác nhận mã danh mục hợp lệ. */
    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy danh mục with ID: " + id);
        }
        // Không xóa Category đang được sử dụng
        long productCount = productRepository.countByCategory_Id(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa danh mục này vì nó đang được sử dụng bởi "
                            + productCount + " sản phẩm."
            );
        }
        categoryRepository.deleteById(id);
    }

    private void validateCategory(Category category) {
        if (category == null || category.getCategoryName() == null
                || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        if (category.getCategoryName().trim().length() > 100) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 100 ký tự.");
        }
        category.setCategoryName(category.getCategoryName().trim());
    }
}
