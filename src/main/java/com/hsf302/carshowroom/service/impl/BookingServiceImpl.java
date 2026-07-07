package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.BookingService;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements com.hsf302.carshowroom.service.BookingService {
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;
    private final SchedulingService schedulingService;

    @Override
    @Transactional
    public Booking createBooking(User user, BookingForm form) {
        if (form.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể đặt lịch trong quá khứ.");
        }

        com.hsf302.carshowroom.entity.Service selectedService = serviceRepository.findById(form.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ."));
        if (selectedService.getStatus() != ServiceStatus.ACTIVE) {
            throw new RuntimeException("Dịch vụ hiện không khả dụng.");
        }

        LocalTime startTime = parseStartTime(form.getTimeSlot());
        LocalTime endTime = startTime.plusMinutes(selectedService.getDurationMinutes());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(resolveVehicle(user, form.getVehicleId()));
        booking.setBookingDate(form.getBookingDate());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setTimeSlot(startTime + " - " + endTime);
        booking.setTotalDurationMinutes(selectedService.getDurationMinutes());
        booking.setEstimatedMinAmount(selectedService.getMinPrice());
        booking.setEstimatedMaxAmount(selectedService.getMaxPrice());
        booking.setDepositAmount(calculateDeposit(selectedService.getMinPrice()));
        booking.setRemainingAmount(selectedService.getMinPrice().subtract(booking.getDepositAmount()));
        booking.setBookingStatus(BookingStatus.PENDING_DEPOSIT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        booking.setNotes(form.getNotes());
        schedulingService.holdSlot(booking);
        Booking savedBooking = bookingRepository.save(booking);

        BookingService snapshot = new BookingService();
        snapshot.setBooking(savedBooking);
        snapshot.setService(selectedService);
        snapshot.setServiceNameSnapshot(selectedService.getServiceName());
        snapshot.setDurationMinutesSnapshot(selectedService.getDurationMinutes());
        snapshot.setMinPriceSnapshot(selectedService.getMinPrice());
        snapshot.setMaxPriceSnapshot(selectedService.getMaxPrice());
        bookingServiceRepository.save(snapshot);

        return savedBooking;
    }

    @Override
    public List<Booking> getBookings(User user) {
        return bookingRepository.findByUserOrderByBookingDateDesc(user);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateDesc();
    }

    @Override
    public Booking getBookingDetail(User user, Integer bookingId) {
        Booking booking = getBookingDetail(bookingId);
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Booking không thuộc tài khoản hiện tại.");
        }
        return booking;
    }

    @Override
    public Booking getBookingDetail(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking."));
    }

    @Override
    @Transactional
    public void cancelBooking(User user, Integer bookingId) {
        Booking booking = getBookingDetail(user, bookingId);
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy lịch đã hoàn tất.");
        }
        booking.setBookingStatus(BookingStatus.CANCELED);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void updateStatus(Integer bookingId, String status) {
        Booking booking = getBookingDetail(bookingId);
        BookingStatus nextStatus = BookingStatus.valueOf(status.toUpperCase());
        booking.setBookingStatus(nextStatus);
        if (nextStatus == BookingStatus.CONFIRMED || nextStatus == BookingStatus.IN_PROGRESS || nextStatus == BookingStatus.COMPLETED) {
            booking.setPaymentStatus(PaymentStatus.PAID);
        }
        bookingRepository.save(booking);
    }

    private Vehicle resolveVehicle(User user, Integer vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe."));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Xe không thuộc tài khoản hiện tại.");
        }
        return vehicle;
    }

    private LocalTime parseStartTime(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            throw new RuntimeException("Vui lòng chọn giờ hẹn.");
        }
        String rawStart = timeSlot.split("-")[0].trim();
        return LocalTime.parse(rawStart);
    }

    private BigDecimal calculateDeposit(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.valueOf(20))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }
}
