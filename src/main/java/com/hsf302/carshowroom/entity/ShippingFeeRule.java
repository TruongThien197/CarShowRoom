package com.hsf302.carshowroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_fee_rule", uniqueConstraints =
        @UniqueConstraint(name = "UX_shipping_fee_rule_region", columnNames = {"province", "district"}))
@Getter
@Setter
@NoArgsConstructor
public class ShippingFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_fee_rule_id")
    private Integer id;

    @Column(name = "province", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String province;

    @Column(name = "district", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String district;

    @Column(name = "fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
