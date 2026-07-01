package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutForm {
    @NotBlank
    private String shippingAddress;

    private String phone;
}
