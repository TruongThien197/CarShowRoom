package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BookingForm {
    private Integer vehicleId;

    @NotNull
    private Integer serviceId;

    @NotNull
    @FutureOrPresent
    private LocalDate bookingDate;

    @NotNull
    @Size(max = 50)
    private String timeSlot;

    private String notes;
}
