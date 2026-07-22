package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByUser(User user);

    long countByCarModel_Id(Integer carModelId);
}
