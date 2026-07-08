package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByUserOrderByBookingDateDesc(User user);

    List<Booking> findAllByOrderByBookingDateDesc();

    List<Booking> findByVehicle(Vehicle vehicle);

    List<Booking> findByBookingDateAndBookingStatusIn(LocalDate bookingDate, List<BookingStatus> statuses);
}
