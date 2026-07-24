package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BookingLifecycleScheduler {
    private final BookingRepository bookingRepository;
    private final OrderWorkflowService orderWorkflowService;
    private final SystemSettingService settingService;
    private final RefundService refundService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    @Transactional
    public void expireNoShowBookings() {
        int graceMinutes = settingService.getInt(
                com.hsf302.carshowroom.service.impl.SystemSettingServiceImpl.NO_SHOW_GRACE_MINUTES);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(graceMinutes);
        for (Booking booking : bookingRepository.findByBookingStatus(BookingStatus.CONFIRMED)) {
            LocalDateTime start = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
            if (!start.isAfter(cutoff)) {
                booking.setBookingStatus(BookingStatus.EXPIRED_NO_SHOW);
                bookingRepository.save(booking);
                int refundPercent = settingService.getInt(
                        com.hsf302.carshowroom.service.impl.SystemSettingServiceImpl.NO_SHOW_REFUND_PERCENT);
                if (booking.getPaymentStatus() == PaymentStatus.PAID && refundPercent > 0) {
                    BigDecimal amount = (booking.getDepositAmount() == null ? BigDecimal.ZERO : booking.getDepositAmount())
                            .multiply(BigDecimal.valueOf(refundPercent)).movePointLeft(2);
                    refundService.requestBookingRefund(booking, amount, "Khách vắng mặt theo lịch hẹn.");
                }
                if (booking.getRelatedOrder() != null) {
                    orderWorkflowService.cancelOrder(booking.getRelatedOrder());
                }
            }
        }
    }
}
