package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.ShippingFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ShippingFeeRuleRepository extends JpaRepository<ShippingFeeRule, Integer> {
    Optional<ShippingFeeRule> findByProvinceAndDistrictAndActiveTrue(String province, String district);

    List<ShippingFeeRule> findByActiveTrueOrderByProvinceAscDistrictAsc();

    boolean existsByProvinceAndDistrict(String province, String district);
}
