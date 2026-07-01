package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByUserOrderByBookingDateDesc(User user);

    List<Booking> findAllByOrderByBookingDateDesc();

    List<Booking> findByVehicle(Vehicle vehicle);
}
