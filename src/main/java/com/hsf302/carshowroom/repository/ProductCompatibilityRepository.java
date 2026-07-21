package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.ProductCompatibility;
import com.hsf302.carshowroom.entity.ProductCompatibilityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCompatibilityRepository extends JpaRepository<ProductCompatibility, ProductCompatibilityId> {
}
