package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.User;

import java.util.List;

public interface BookingService {
    Booking createBooking(User user, BookingForm form);

    List<Booking> getBookings(User user);

    List<Booking> getAllBookings();

    Booking getBookingDetail(User user, Integer bookingId);

    Booking getBookingDetail(Integer bookingId);

    void cancelBooking(User user, Integer bookingId);

    void updateStatus(Integer bookingId, String status);
}
