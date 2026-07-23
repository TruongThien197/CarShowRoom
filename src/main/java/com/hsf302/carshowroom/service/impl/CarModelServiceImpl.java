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

    /** Lấy danh sách dòng xe theo thứ tự hãng, tên mẫu và năm sản xuất. */
    @Override
    public List<CarModel> getAllCarModels() {
        return carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc();
    }

    /** Lọc các dòng xe theo hãng; nếu không có hãng thì trả về toàn bộ danh sách. */
    @Override
    public List<CarModel> getModelsByBrand(String brand) {
        if (!StringUtils.hasText(brand)) {
            return getAllCarModels();
        }
        return carModelRepository.findByBrandIgnoreCaseOrderByModelNameAsc(brand.trim());
    }

    /** Lấy chi tiết một dòng xe theo mã. */
    @Override
    public CarModel getCarModel(Integer id) {
        return carModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu xe"));
    }

    /** Tạo dòng xe mới từ dữ liệu biểu mẫu quản trị. */
    @Override
    @Transactional
    public CarModel createCarModel(CarModelForm form) {
        CarModel carModel = new CarModel();
        fillCarModel(carModel, form);
        return carModelRepository.save(carModel);
    }

    /** Cập nhật thông tin dòng xe theo mã. */
    @Override
    @Transactional
    public CarModel updateCarModel(Integer id, CarModelForm form) {
        CarModel carModel = getCarModel(id);
        fillCarModel(carModel, form);
        return carModelRepository.save(carModel);
    }

    /** Xóa dòng xe theo mã. */
    @Override
    @Transactional
    public void deleteCarModel(Integer id) {
        long productCount = productRepository.countByCompatibleCarModels_Id(id);
        long vehicleCount = vehicleRepository.countByCarModel_Id(id);
        if (productCount > 0 || vehicleCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa dòng xe này vì nó đang được sử dụng bởi các sản phẩm hoặc phương tiện của khách hàng."
            );
        }
        carModelRepository.deleteById(id);
    }

    /** Sao chép các trường hãng, tên mẫu và năm từ biểu mẫu vào thực thể dòng xe. */
    private void fillCarModel(CarModel carModel, CarModelForm form) {
        validateCarModel(form);
        carModel.setBrand(form.getBrand());
        carModel.setModelName(form.getModelName());
        carModel.setYear(form.getYear());
    }

    private void validateCarModel(CarModelForm form) {
        if (form == null || form.getBrand() == null || form.getBrand().isBlank()
                || form.getModelName() == null || form.getModelName().isBlank()) {
            throw new IllegalArgumentException("Tên hãng và tên dòng xe không được để trống.");
        }
        if (form.getBrand().trim().length() > 100
                || form.getModelName().trim().length() > 100) {
            throw new IllegalArgumentException("Tên hãng và tên dòng xe không được vượt quá 100 ký tự.");
        }
        if (form.getYear() == null || form.getYear() < 1900
                || form.getYear() > java.time.LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("Năm sản xuất dòng xe không hợp lệ.");
        }
        form.setBrand(form.getBrand().trim());
        form.setModelName(form.getModelName().trim());
    }
}
