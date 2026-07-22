package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.common.Enums.ProductStatus;
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
    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryIdAndStatus(Integer categoryId, ProductStatus status);

    List<Product> findByNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status);

    Optional<Product> findBySku(String sku);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(Integer categoryId, ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndNameContainingIgnoreCaseAndStatus(Integer categoryId, String keyword, ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryId(Integer categoryId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(Integer categoryId, String keyword, Pageable pageable);

    @Query("""
            select distinct p from Product p
            left join p.compatibleCarModels cm
            where (:categoryId is null or p.category.id = :categoryId)
              and p.status = com.hsf302.carshowroom.common.Enums.ProductStatus.ACTIVE
              and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:carModelId is null or cm.id = :carModelId)
              and (:brand is null or lower(cm.brand) = lower(:brand))
              and (:modelName is null or lower(cm.modelName) = lower(:modelName))
              and (:year is null or cm.year = :year)
            """)
    Page<Product> searchCatalog(@Param("categoryId") Integer categoryId,
                                @Param("keyword") String keyword,
                                @Param("carModelId") Integer carModelId,
                                @Param("brand") String brand,
                                @Param("modelName") String modelName,
                                @Param("year") Integer year,
                                Pageable pageable);

    @Query("""
            select distinct p from Product p
            left join p.compatibleCarModels cm
            where (:categoryId is null or p.category.id = :categoryId)
              and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:carModelId is null or cm.id = :carModelId)
              and (:brand is null or lower(cm.brand) = lower(:brand))
              and (:modelName is null or lower(cm.modelName) = lower(:modelName))
              and (:year is null or cm.year = :year)
            """)
    Page<Product> searchAdminCatalog(@Param("categoryId") Integer categoryId,
                                     @Param("keyword") String keyword,
                                     @Param("carModelId") Integer carModelId,
                                     @Param("brand") String brand,
                                     @Param("modelName") String modelName,
                                     @Param("year") Integer year,
                                     Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Integer id);
}
