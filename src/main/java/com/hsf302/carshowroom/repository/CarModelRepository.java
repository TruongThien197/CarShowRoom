package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Integer> {
    List<CarModel> findByBrandIgnoreCaseOrderByModelNameAsc(String brand);

    List<CarModel> findAllByOrderByBrandAscModelNameAscYearDesc();

    List<CarModel> findByBrandIgnoreCaseAndModelNameIgnoreCaseAndYear(String brand, String modelName, Integer year);

    @Query("select distinct c.brand from CarModel c order by c.brand")
    List<String> findDistinctBrands();

    List<CarModel> findByBrandOrderByModelNameAscYearDesc(String brand);
}
