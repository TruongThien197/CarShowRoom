package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Nationalized
    @Column(name = "product_name", nullable = false, length = 150)
    private String name;

    @Column(unique = true)
    private String sku;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer physicalStock = 0;

    @Column(name = "reserved_stock")
    private Integer reservedStock = 0;

    @Nationalized
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Nationalized
    @Lob
    private String description;

    @ManyToMany
    @JoinTable(
            name = "product_car_models",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "car_model_id")
    )
    private Set<CarModel> compatibleCarModels = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Nationalized
    @Column(nullable = false, length = 50)
    private ProductStatus status;

    @Version
    @Column(nullable = true)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = true)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Transient
    public int getAvailableStock() {
        return nullToZero(physicalStock) - nullToZero(reservedStock);
    }

    @Transient
    public String getProductName() {
        return name;
    }

    @Transient
    public Integer getStockQuantity() {
        return getAvailableStock();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
