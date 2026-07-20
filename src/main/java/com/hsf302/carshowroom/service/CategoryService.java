package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();

    Category getCategoryById(Integer id);

    Category createCategory(Category category);

    Category updateCategory(Integer id, Category updatedCategory);

    void deleteCategory(Integer id);
}
