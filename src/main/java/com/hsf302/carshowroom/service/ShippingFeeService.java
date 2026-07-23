package com.hsf302.carshowroom.service;

import java.math.BigDecimal;

public interface ShippingFeeService {
    BigDecimal resolveFee(String province, String district);
}
