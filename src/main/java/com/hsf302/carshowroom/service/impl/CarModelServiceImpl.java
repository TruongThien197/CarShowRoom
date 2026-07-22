package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.dto.CarModelForm;
import com.hsf302.carshowroom.entity.CarModel;
import com.hsf302.carshowroom.repository.CarModelRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
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
    private final ProductRepository productRepository;
    private final VehicleRepository vehicleRepository;

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
        long productCount = productRepository.countByCompatibleCarModels_Id(id);
        long vehicleCount = vehicleRepository.countByCarModel_Id(id);
        if (productCount > 0 || vehicleCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete this car model because it is being used by products or customer vehicles."
            );
        }
        carModelRepository.deleteById(id);
    }

    private void fillCarModel(CarModel carModel, CarModelForm form) {
        validateCarModel(form);
        carModel.setBrand(form.getBrand());
        carModel.setModelName(form.getModelName());
        carModel.setYear(form.getYear());
    }

    private void validateCarModel(CarModelForm form) {
        if (form == null || form.getBrand() == null || form.getBrand().isBlank()
                || form.getModelName() == null || form.getModelName().isBlank()) {
            throw new IllegalArgumentException("Brand and model name must not be empty.");
        }
        if (form.getBrand().trim().length() > 100
                || form.getModelName().trim().length() > 100) {
            throw new IllegalArgumentException("Brand and model name must not exceed 100 characters.");
        }
        if (form.getYear() == null || form.getYear() < 1900
                || form.getYear() > java.time.LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("Car model year is invalid.");
        }
        form.setBrand(form.getBrand().trim());
        form.setModelName(form.getModelName().trim());
    }
}
