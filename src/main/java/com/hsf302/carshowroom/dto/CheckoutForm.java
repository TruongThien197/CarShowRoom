package com.hsf302.carshowroom.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CheckoutForm {
    private String shippingProvince;

    private String shippingDistrict;

    private String shippingWard;

    private String shippingAddress;

    private String phone;

    private String paymentMethod = "PAYOS";

    private Integer vehicleId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private String notes;
}
