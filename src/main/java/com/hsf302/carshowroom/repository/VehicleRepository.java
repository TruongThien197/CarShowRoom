package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
}
