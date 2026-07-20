package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.dto.CarModelForm;
import com.hsf302.carshowroom.entity.CarModel;
import com.hsf302.carshowroom.repository.CarModelRepository;
import com.hsf302.carshowroom.service.CarModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarModelServiceImpl implements CarModelService {
    private final CarModelRepository carModelRepository;

    @Override
    public List<CarModel> getAllCarModels() {
        return carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc();
    }

    @Override
    public List<CarModel> getModelsByBrand(String brand) {
        if (!StringUtils.hasText(brand)) {
            return getAllCarModels();
        }
        return carModelRepository.findByBrandIgnoreCaseOrderByModelNameAsc(brand.trim());
    }

    @Override
    public CarModel getCarModel(Integer id) {
        return carModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu xe"));
    }

    @Override
    @Transactional
    public CarModel createCarModel(CarModelForm form) {
        CarModel carModel = new CarModel();
        fillCarModel(carModel, form);
        return carModelRepository.save(carModel);
    }

    @Override
    @Transactional
    public CarModel updateCarModel(Integer id, CarModelForm form) {
        CarModel carModel = getCarModel(id);
        fillCarModel(carModel, form);
        return carModelRepository.save(carModel);
    }

    @Override
    @Transactional
    public void deleteCarModel(Integer id) {
        carModelRepository.deleteById(id);
    }

    private void fillCarModel(CarModel carModel, CarModelForm form) {
        carModel.setBrand(form.getBrand());
        carModel.setModelName(form.getModelName());
        carModel.setYear(form.getYear());
    }
}
