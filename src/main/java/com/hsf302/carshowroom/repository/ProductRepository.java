package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByStatusIgnoreCase(String status);

    List<Product> findByCategoryIdAndStatusIgnoreCase(Integer categoryId, String status);

    List<Product> findByProductNameContainingIgnoreCaseAndStatusIgnoreCase(String keyword, String status);

    Page<Product> findByStatusIgnoreCase(String status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatusIgnoreCase(Integer categoryId, String status, Pageable pageable);

    Page<Product> findByProductNameContainingIgnoreCaseAndStatusIgnoreCase(String keyword, String status, Pageable pageable);

    Page<Product> findByCategoryIdAndProductNameContainingIgnoreCaseAndStatusIgnoreCase(Integer categoryId, String keyword, String status, Pageable pageable);

    Page<Product> findByCategoryId(Integer categoryId, Pageable pageable);

    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByCategoryIdAndProductNameContainingIgnoreCase(Integer categoryId, String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Integer id);
}