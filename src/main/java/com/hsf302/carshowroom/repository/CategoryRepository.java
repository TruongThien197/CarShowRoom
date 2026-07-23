package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findByCategoryNameIgnoreCase(String categoryName);
}
