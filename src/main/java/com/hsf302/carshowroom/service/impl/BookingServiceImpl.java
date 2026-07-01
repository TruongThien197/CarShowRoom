package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.BookingDetail;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.repository.BookingDetailRepository;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public Booking createBooking(User user, BookingForm form) {
        if (form.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Booking date cannot be in the past");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(resolveVehicle(user, form.getVehicleId()));
        booking.setBookingDate(form.getBookingDate());
        booking.setTimeSlot(form.getTimeSlot());
        booking.setStatus("PENDING");
        booking.setNotes(form.getNotes());
        Booking savedBooking = bookingRepository.save(booking);

        com.hsf302.carshowroom.entity.Service selectedService = serviceRepository.findById(form.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));
        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setService(selectedService);
        detail.setActualPrice(selectedService.getPrice());
        bookingDetailRepository.save(detail);

        return savedBooking;
    }

    @Override
    public List<Booking> getBookings(User user) {
        return bookingRepository.findByUserOrderByBookingDateDesc(user);
    }

    private Vehicle resolveVehicle(User user, Integer vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vehicle does not belong to current user");
        }
        return vehicle;
    }
}
