package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.ShippingFeeRule;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingFeeRuleManagementService {
    List<ShippingFeeRule> getAll();
    ShippingFeeRule create(String province, String district, BigDecimal fee);
    void update(Integer id, BigDecimal fee, boolean active);
}
