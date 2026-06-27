package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
}
