package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.VehicleForm;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;

import java.util.List;

public interface VehicleService {
    Vehicle addVehicle(User user, VehicleForm form);

    List<Vehicle> getVehicles(User user);

    void deleteVehicle(User user, Integer vehicleId);
}
