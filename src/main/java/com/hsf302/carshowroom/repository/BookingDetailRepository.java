package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {
    List<BookingDetail> findByBookingId(Integer bookingId);
}
