package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.repository.CategoryRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private CategoryServiceImpl categoryService;

    @Test
    void createCategoryTrimsName() {
        Category category = new Category();
        category.setCategoryName("  Brakes  ");
        when(categoryRepository.save(category)).thenReturn(category);

        Category result = categoryService.createCategory(category);

        assertEquals("Brakes", result.getCategoryName());
        verify(categoryRepository).save(category);
    }

    @Test
    void createCategoryRejectsBlankName() {
        Category category = new Category();
        category.setCategoryName(" ");

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(category));
        verify(categoryRepository, never()).save(category);
    }

    @Test
    void deleteCategoryRejectsCategoryUsedByProduct() {
        when(categoryRepository.existsById(7)).thenReturn(true);
        when(productRepository.countByCategory_Id(7)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> categoryService.deleteCategory(7));
        verify(categoryRepository, never()).deleteById(7);
    }

    @Test
    void deleteCategoryDeletesUnusedCategory() {
        when(categoryRepository.existsById(7)).thenReturn(true);
        when(productRepository.countByCategory_Id(7)).thenReturn(0L);

        categoryService.deleteCategory(7);

        verify(categoryRepository).deleteById(7);
    }
}
