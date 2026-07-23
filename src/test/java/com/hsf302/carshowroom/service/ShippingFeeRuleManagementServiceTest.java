package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.ShippingFeeRule;
import com.hsf302.carshowroom.repository.ShippingFeeRuleRepository;
import com.hsf302.carshowroom.service.impl.ShippingFeeRuleManagementServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingFeeRuleManagementServiceTest {
    private final ShippingFeeRuleRepository repository = mock(ShippingFeeRuleRepository.class);
    private final ShippingFeeRuleManagementService service = new ShippingFeeRuleManagementServiceImpl(repository);

    @Test
    void createsAnActiveRuleWithNormalizedRegionNames() {
        when(repository.existsByProvinceAndDistrict("Đà Nẵng", "Hải Châu")).thenReturn(false);
        when(repository.save(any(ShippingFeeRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShippingFeeRule rule = service.create("  Đà Nẵng ", " Hải Châu ", BigDecimal.valueOf(35_000));

        assertEquals("Đà Nẵng", rule.getProvince());
        assertEquals("Hải Châu", rule.getDistrict());
        assertEquals(BigDecimal.valueOf(35_000), rule.getFee());
        verify(repository).save(rule);
    }

    @Test
    void refusesToCreateTheSameProvinceAndDistrictTwice() {
        when(repository.existsByProvinceAndDistrict("Đà Nẵng", "Hải Châu")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("Đà Nẵng", "Hải Châu", BigDecimal.TEN));
    }
}
