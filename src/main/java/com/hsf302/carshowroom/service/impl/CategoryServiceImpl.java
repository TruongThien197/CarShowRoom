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

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục with ID: " + id));
    }

    @Override
    public Category createCategory(Category category) {
        validateCategory(category);
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Integer id, Category updatedCategory) {
        Category existing = getCategoryById(id);
        validateCategory(updatedCategory);
        existing.setCategoryName(updatedCategory.getCategoryName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

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
                    "Cannot delete this category because it is used by "
                            + productCount + " product(s)."
            );
        }
        categoryRepository.deleteById(id);
    }

    private void validateCategory(Category category) {
        if (category == null || category.getCategoryName() == null
                || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Category name must not be empty.");
        }
        if (category.getCategoryName().trim().length() > 100) {
            throw new IllegalArgumentException("Category name must not exceed 100 characters.");
        }
        category.setCategoryName(category.getCategoryName().trim());
    }
}
