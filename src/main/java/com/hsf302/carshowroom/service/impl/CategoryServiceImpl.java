package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.repository.CategoryRepository;
import com.hsf302.carshowroom.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

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
        return categoryRepository.save(category);
    }

    /** Cập nhật tên và mô tả của danh mục đã tồn tại. */
    @Override
    public Category updateCategory(Integer id, Category updatedCategory) {
        Category existing = getCategoryById(id);
        existing.setCategoryName(updatedCategory.getCategoryName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

    /** Xóa danh mục sau khi xác nhận mã danh mục hợp lệ. */
    @Override
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy danh mục with ID: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
