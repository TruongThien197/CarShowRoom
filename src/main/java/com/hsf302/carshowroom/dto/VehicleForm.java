package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleForm {
    @NotBlank
    @Size(max = 100)
    private String brand;

    @NotBlank
    @Size(max = 100)
    private String modelName;

    @NotNull
    @Min(1886)
    @Max(2030)
    private Integer year;

    @NotBlank
    @Size(max = 30)
    private String licensePlate;
}
