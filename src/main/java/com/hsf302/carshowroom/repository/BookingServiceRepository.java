package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingServiceRepository extends JpaRepository<BookingService, Integer> {
    List<BookingService> findByBookingId(Integer bookingId);
}
