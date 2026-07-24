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
    void approveCancellation(Integer bookingId, User processedBy, String assessmentNote);
    void rejectCancellation(Integer bookingId, User processedBy, String reason);

    void updateStatus(Integer bookingId, String status);

    /** Xác nhận nhân viên đã chuyển khoản hoàn cọc thủ công và lưu mã giao dịch. */
    void completeRefund(Integer bookingId, User processedBy, String transactionCode);

    void submitRefundAccount(User user, Integer bookingId, String bankName,
                             String accountHolder, String accountNumber);

    void checkIn(Integer bookingId);

    void setFinalAmount(Integer bookingId, java.math.BigDecimal finalAmount);

    void recordLaborCollection(Integer bookingId, User staff, java.math.BigDecimal laborFee);

    void markNoShow(Integer bookingId);
    void expireDepositPaymentIfDue(Integer bookingId);
    void reopenDepositPayment(Integer bookingId);
}
