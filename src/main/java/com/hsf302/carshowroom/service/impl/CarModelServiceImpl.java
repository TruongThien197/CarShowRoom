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
        carModelRepository.deleteById(id);
    }

    /** Sao chép các trường hãng, tên mẫu và năm từ biểu mẫu vào thực thể dòng xe. */
    private void fillCarModel(CarModel carModel, CarModelForm form) {
        carModel.setBrand(form.getBrand());
        carModel.setModelName(form.getModelName());
        carModel.setYear(form.getYear());
    }
}
