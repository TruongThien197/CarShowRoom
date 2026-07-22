package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CarModelForm;
import com.hsf302.carshowroom.entity.CarModel;
import com.hsf302.carshowroom.repository.CarModelRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.impl.CarModelServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarModelServiceImplTest {
    @Mock private CarModelRepository carModelRepository;
    @Mock private ProductRepository productRepository;
    @Mock private VehicleRepository vehicleRepository;
    @InjectMocks private CarModelServiceImpl carModelService;

    @Test
    void createCarModelTrimsFields() {
        CarModelForm form = validForm(" Toyota ", " Camry ", 2024);
        when(carModelRepository.save(org.mockito.ArgumentMatchers.any(CarModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CarModel result = carModelService.createCarModel(form);

        assertEquals("Toyota", result.getBrand());
        assertEquals("Camry", result.getModelName());
        verify(carModelRepository).save(org.mockito.ArgumentMatchers.any(CarModel.class));
    }

    @Test
    void createCarModelRejectsInvalidYear() {
        CarModelForm form = validForm("Toyota", "Camry", 1899);

        assertThrows(IllegalArgumentException.class, () -> carModelService.createCarModel(form));
        verify(carModelRepository, never()).save(org.mockito.ArgumentMatchers.any(CarModel.class));
    }

    @Test
    void deleteCarModelRejectsModelUsedByProductOrVehicle() {
        when(productRepository.countByCompatibleCarModels_Id(3)).thenReturn(1L);
        when(vehicleRepository.countByCarModel_Id(3)).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> carModelService.deleteCarModel(3));
        verify(carModelRepository, never()).deleteById(3);
    }

    @Test
    void deleteCarModelDeletesUnusedModel() {
        when(productRepository.countByCompatibleCarModels_Id(3)).thenReturn(0L);
        when(vehicleRepository.countByCarModel_Id(3)).thenReturn(0L);

        carModelService.deleteCarModel(3);

        verify(carModelRepository).deleteById(3);
    }

    private CarModelForm validForm(String brand, String modelName, Integer year) {
        CarModelForm form = new CarModelForm();
        form.setBrand(brand);
        form.setModelName(modelName);
        form.setYear(year);
        return form;
    }
}
