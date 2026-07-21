package com.hsf302.carshowroom.dto;

import com.hsf302.carshowroom.entity.CartItem;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartDTO {
    private List<CartItem> items = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
}
