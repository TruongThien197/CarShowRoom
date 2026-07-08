package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CheckoutForm {
    @NotBlank
    private String shippingAddress;

    private String phone;

    private Integer vehicleId;

    private Integer serviceId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private String notes;
}
