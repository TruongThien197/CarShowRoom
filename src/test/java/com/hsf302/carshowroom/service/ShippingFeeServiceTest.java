package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.ShippingFeeRule;
import com.hsf302.carshowroom.repository.ShippingFeeRuleRepository;
import com.hsf302.carshowroom.service.impl.ShippingFeeServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShippingFeeServiceTest {

    private final ShippingFeeRuleRepository repository = mock(ShippingFeeRuleRepository.class);
    private final ShippingFeeService shippingFeeService = new ShippingFeeServiceImpl(repository);

    @Test
    void returnsTheActiveFeeForTheSelectedProvinceAndDistrict() {
        ShippingFeeRule rule = new ShippingFeeRule();
        rule.setFee(BigDecimal.valueOf(35_000));
        when(repository.findByProvinceAndDistrictAndActiveTrue("Đà Nẵng", "Hải Châu"))
                .thenReturn(Optional.of(rule));

        assertEquals(BigDecimal.valueOf(35_000),
                shippingFeeService.resolveFee("  Đà Nẵng  ", " Hải Châu "));
    }

    @Test
    void rejectsARegionWithoutAnActiveFeeRule() {
        when(repository.findByProvinceAndDistrictAndActiveTrue("Đắk Lắk", "Buôn Hồ"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> shippingFeeService.resolveFee("Đắk Lắk", "Buôn Hồ"));
    }
}
