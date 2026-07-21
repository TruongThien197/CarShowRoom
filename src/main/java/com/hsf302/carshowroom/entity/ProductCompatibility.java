package com.hsf302.carshowroom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_compatibility")
public class ProductCompatibility {
    @EmbeddedId
    private ProductCompatibilityId id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @MapsId("carModelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_model_id", nullable = false)
    private CarModel carModel;


}
