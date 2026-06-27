package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarModelRepository extends JpaRepository<CarModel, Integer> {
}
