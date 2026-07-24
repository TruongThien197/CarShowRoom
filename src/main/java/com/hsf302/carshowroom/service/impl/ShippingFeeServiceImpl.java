package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.repository.ShippingFeeRuleRepository;
import com.hsf302.carshowroom.service.ShippingFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ShippingFeeServiceImpl implements ShippingFeeService {
    private final ShippingFeeRuleRepository shippingFeeRuleRepository;

    @Override
    public BigDecimal resolveFee(String province, String district) {
        String resolvedProvince = requireText(province, "Vui lòng chọn tỉnh/thành phố giao hàng.");
        String resolvedDistrict = requireText(district, "Vui lòng chọn quận/huyện giao hàng.");
        return shippingFeeRuleRepository.findByProvinceAndDistrictAndActiveTrue(resolvedProvince, resolvedDistrict)
                .map(rule -> rule.getFee() == null ? BigDecimal.ZERO : rule.getFee())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khu vực giao hàng này chưa được cấu hình phí ship. Vui lòng chọn khu vực khác hoặc liên hệ cửa hàng."));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
