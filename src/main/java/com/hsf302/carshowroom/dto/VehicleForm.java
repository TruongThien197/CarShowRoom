package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleForm {
    private Integer carModelId;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String modelName;

    private Integer year;

    @NotBlank
    @Size(max = 30)
    private String licensePlate;

    @AssertTrue(message = "Hãy chọn dòng xe hoặc nhập hãng, tên xe và năm sản xuất.")
    public boolean isVehicleInformationValid() {
        if (carModelId != null) {
            return true;
        }
        return brand != null && !brand.isBlank()
                && modelName != null && !modelName.isBlank()
                && year != null && year >= 1886 && year <= 2100;
    }
}
