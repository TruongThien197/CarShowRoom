package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.ShippingFeeRule;
import com.hsf302.carshowroom.repository.ShippingFeeRuleRepository;
import com.hsf302.carshowroom.service.ShippingFeeRuleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingFeeRuleManagementServiceImpl implements ShippingFeeRuleManagementService {
    private final ShippingFeeRuleRepository shippingFeeRuleRepository;

    @Override
    public List<ShippingFeeRule> getAll() {
        return shippingFeeRuleRepository.findAll();
    }

    @Override
    @Transactional
    public ShippingFeeRule create(String province, String district, BigDecimal fee) {
        String normalizedProvince = requireText(province, "Vui lòng nhập tỉnh/thành phố.");
        String normalizedDistrict = requireText(district, "Vui lòng nhập quận/huyện.");
        BigDecimal normalizedFee = requireFee(fee);
        if (shippingFeeRuleRepository.existsByProvinceAndDistrict(normalizedProvince, normalizedDistrict)) {
            throw new IllegalArgumentException("Khu vực này đã có rule phí ship. Hãy cập nhật rule hiện có.");
        }
        ShippingFeeRule rule = new ShippingFeeRule();
        rule.setProvince(normalizedProvince);
        rule.setDistrict(normalizedDistrict);
        rule.setFee(normalizedFee);
        rule.setActive(true);
        return shippingFeeRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void update(Integer id, BigDecimal fee, boolean active) {
        ShippingFeeRule rule = shippingFeeRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rule phí ship."));
        rule.setFee(requireFee(fee));
        rule.setActive(active);
        shippingFeeRuleRepository.save(rule);
    }

    private BigDecimal requireFee(BigDecimal fee) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí ship phải lớn hơn hoặc bằng 0.");
        }
        return fee;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
