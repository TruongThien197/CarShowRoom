package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.ShippingFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingFeeRuleRepository extends JpaRepository<ShippingFeeRule, Integer> {
    Optional<ShippingFeeRule> findByProvinceAndDistrictAndActiveTrue(String province, String district);

    boolean existsByProvinceAndDistrict(String province, String district);
}
