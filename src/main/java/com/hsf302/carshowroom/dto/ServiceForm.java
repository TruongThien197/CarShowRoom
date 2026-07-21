package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceForm {
    @NotBlank
    @Size(max = 150)
    private String serviceName;

    private String description;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal minPrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal maxPrice;

    @NotNull
    private Integer durationMinutes;
}
