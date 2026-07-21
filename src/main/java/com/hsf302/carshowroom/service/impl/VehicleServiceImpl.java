package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.dto.VehicleForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Vehicle addVehicle(User user, VehicleForm form) {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setBrand(form.getBrand().trim());
        vehicle.setModelName(form.getModelName().trim());
        vehicle.setYear(form.getYear());
        vehicle.setLicensePlate(form.getLicensePlate());
        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getVehicles(User user) {
        return vehicleRepository.findByUser(user);
    }

    @Override
    @Transactional
    public void deleteVehicle(User user, Integer vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vehicle does not belong to current user");
        }
        for (Booking booking : bookingRepository.findByVehicle(vehicle)) {
            booking.setVehicle(null);
            bookingRepository.save(booking);
        }
        vehicleRepository.delete(vehicle);
    }
}
