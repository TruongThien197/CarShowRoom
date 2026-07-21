package com.hsf302.carshowroom.dto;

import lombok.Data;

@Data
public class CheckoutResponseDTO {
    private String checkoutUrl;
    private Integer orderId;
    private Integer bookingId;
}
