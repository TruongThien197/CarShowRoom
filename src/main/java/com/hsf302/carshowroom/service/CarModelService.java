package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CarModelForm;
import com.hsf302.carshowroom.entity.CarModel;

import java.util.List;

public interface CarModelService {
    List<CarModel> getAllCarModels();

    List<CarModel> getModelsByBrand(String brand);

    CarModel getCarModel(Integer id);

    CarModel createCarModel(CarModelForm form);

    CarModel updateCarModel(Integer id, CarModelForm form);

    void deleteCarModel(Integer id);
}
