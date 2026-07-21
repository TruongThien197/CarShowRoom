package com.hsf302.carshowroom.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    private Integer vehicleId;
    private List<Integer> serviceIds;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private String notes;
    private String shippingAddress;
}
