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
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Integer id, Category updatedCategory) {
        Category existing = getCategoryById(id);
        existing.setCategoryName(updatedCategory.getCategoryName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

    @Override
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy danh mục with ID: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
