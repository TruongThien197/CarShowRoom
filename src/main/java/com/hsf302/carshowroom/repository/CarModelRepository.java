package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Integer> {
    List<CarModel> findByBrandIgnoreCaseOrderByModelNameAsc(String brand);

    List<CarModel> findAllByOrderByBrandAscModelNameAscYearDesc();
}
