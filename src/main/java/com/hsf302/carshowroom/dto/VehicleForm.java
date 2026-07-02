package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleForm {
    @NotNull
    private Integer carModelId;

    @NotBlank
    @Size(max = 30)
    private String licensePlate;
}
