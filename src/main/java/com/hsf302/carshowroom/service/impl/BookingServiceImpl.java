package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
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
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements com.hsf302.carshowroom.service.BookingService {
    private static final BigDecimal DEPOSIT_RATE = BigDecimal.valueOf(0.20);
    private static final BigDecimal MIN_DEPOSIT = BigDecimal.valueOf(2_000);
    private static final BigDecimal MAX_DEPOSIT = BigDecimal.valueOf(10_000);

    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;
    private final SchedulingService schedulingService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional
    public Booking createBooking(User user, BookingForm form) {
        if (form.getVehicleId() == null) {
            throw new RuntimeException("Vui lòng chọn xe trước khi đặt lịch.");
        }
        if (form.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Bạn không thể đặt lịch trong quá khứ.");
        }

        com.hsf302.carshowroom.entity.Service selectedService = serviceRepository.findById(form.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ."));
        if (selectedService.getStatus() != ServiceStatus.ACTIVE) {
            throw new RuntimeException("Dịch vụ này hiện không khả dụng.");
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
        booking.setFinalAmount(selectedService.getMinPrice());
        booking.setDepositAmount(calculateDeposit(selectedService.getMinPrice()));
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
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

    private BigDecimal calculateDeposit(BigDecimal estimatedMinAmount) {
        if (estimatedMinAmount == null) {
            return MIN_DEPOSIT;
        }
        return estimatedMinAmount.multiply(DEPOSIT_RATE)
                .max(MIN_DEPOSIT)
                .min(MAX_DEPOSIT)
                .setScale(0, java.math.RoundingMode.UP);
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
            throw new RuntimeException("Lịch hẹn này không thuộc tài khoản của bạn.");
        }
        return booking;
    }

    @Override
    public Booking getBookingDetail(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));
    }

    @Override
    @Transactional
    public void cancelBooking(User user, Integer bookingId) {
        Booking booking = getBookingDetail(user, bookingId);
        if (booking.getBookingStatus() == BookingStatus.IN_PROGRESS
                || booking.getBookingStatus() == BookingStatus.COMPLETED
                || booking.getBookingStatus() == BookingStatus.CANCELED
                || booking.getBookingStatus() == BookingStatus.EXPIRED_PAYMENT
                || booking.getBookingStatus() == BookingStatus.EXPIRED_NO_SHOW) {
            throw new RuntimeException("Không thể hủy lịch hẹn ở trạng thái hiện tại.");
        }
        booking.setBookingStatus(BookingStatus.CANCELED);
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                && booking.getRefundStatus() == RefundStatus.NONE) {
            booking.setRefundStatus(RefundStatus.REQUESTED);
        }
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void completeRefund(Integer bookingId, User processedBy, String bankName,
                               String accountHolder, String accountNumber, String note) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getBookingStatus() != BookingStatus.CANCELED
                || booking.getRefundStatus() != RefundStatus.REQUESTED
                || booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Lịch hẹn không có yêu cầu hoàn tiền đang chờ xử lý.");
        }
        if (bankName == null || bankName.isBlank()
                || accountHolder == null || accountHolder.isBlank()
                || accountNumber == null || accountNumber.isBlank()
                || note == null || note.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ ngân hàng, chủ tài khoản, số tài khoản và mã giao dịch.");
        }
        if (!samePerson(accountHolder, booking.getUser().getFullName())) {
            throw new IllegalArgumentException("Tên người nhận tiền phải trùng với khách hàng đã thanh toán tiền cọc.");
        }
        booking.setRefundStatus(RefundStatus.COMPLETED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        booking.setRefundNote(note.trim());
        booking.setRefundBankName(bankName.trim());
        booking.setRefundAccountHolder(accountHolder.trim());
        booking.setRefundAccountNumber(accountNumber.trim());
        booking.setRefundedAt(LocalDateTime.now());
        booking.setRefundedBy(processedBy);
        paymentTransactionRepository.findByBooking(booking).forEach(transaction -> {
            transaction.setStatus(PaymentStatus.REFUNDED);
            paymentTransactionRepository.save(transaction);
        });
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void submitRefundAccount(User user, Integer bookingId, String bankName,
                                    String accountHolder, String accountNumber) {
        Booking booking = getBookingDetail(user, bookingId);
        if (booking.getBookingStatus() != BookingStatus.CANCELED
                || booking.getRefundStatus() != RefundStatus.REQUESTED
                || booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Booking này không cần bổ sung thông tin hoàn tiền.");
        }
        if (bankName == null || bankName.isBlank()
                || accountHolder == null || accountHolder.isBlank()
                || accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin tài khoản nhận tiền.");
        }
        if (!samePerson(accountHolder, booking.getUser().getFullName())) {
            throw new IllegalArgumentException("Tên người nhận tiền phải trùng với tên khách hàng đã thanh toán tiền cọc.");
        }
        booking.setRefundBankName(bankName.trim());
        booking.setRefundAccountHolder(booking.getUser().getFullName());
        booking.setRefundAccountNumber(accountNumber.trim());
        bookingRepository.save(booking);
    }

    private boolean samePerson(String first, String second) {
        return first.trim().replaceAll("\\s+", " ").equalsIgnoreCase(
                second == null ? "" : second.trim().replaceAll("\\s+", " "));
    }

    @Override
    @Transactional
    public void updateStatus(Integer bookingId, String status) {
        Booking booking = getBookingDetail(bookingId);
        BookingStatus nextStatus = BookingStatus.valueOf(status.toUpperCase());
        if (booking.getBookingStatus() == nextStatus) {
            return;
        }
        validateStatusTransition(booking, nextStatus);
        booking.setBookingStatus(nextStatus);
        bookingRepository.save(booking);
    }

    private void validateStatusTransition(Booking booking, BookingStatus nextStatus) {
        BookingStatus currentStatus = booking.getBookingStatus();
        if (nextStatus == BookingStatus.CANCELED) {
            if (currentStatus == BookingStatus.COMPLETED
                    || currentStatus == BookingStatus.CANCELED
                    || currentStatus == BookingStatus.EXPIRED_PAYMENT
                    || currentStatus == BookingStatus.EXPIRED_NO_SHOW) {
                throw new IllegalStateException("Không thể hủy lịch hẹn ở trạng thái hiện tại.");
            }
            return;
        }
        if (nextStatus == BookingStatus.CONFIRMED) {
            boolean waiting = currentStatus == BookingStatus.CREATED
                    || currentStatus == BookingStatus.PENDING_PAYMENT
                    || currentStatus == BookingStatus.PENDING_APPROVAL;
            if (!waiting) {
                throw new IllegalStateException("Chỉ lịch hẹn đang chờ mới có thể được xác nhận.");
            }
            if (booking.getPaymentStatus() != PaymentStatus.PAID) {
                throw new IllegalStateException("Lịch hẹn chỉ được tự động xác nhận sau khi khách thanh toán tiền cọc.");
            }
            return;
        }
        if (nextStatus == BookingStatus.IN_PROGRESS && currentStatus != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Lịch hẹn phải được xác nhận trước khi bắt đầu.");
        }
        if (nextStatus == BookingStatus.COMPLETED && currentStatus != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Lịch hẹn phải ở trạng thái đang thực hiện trước khi hoàn tất.");
        }
        if (nextStatus != BookingStatus.IN_PROGRESS && nextStatus != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Trạng thái lịch hẹn không được phép cập nhật thủ công.");
        }
    }

    private Vehicle resolveVehicle(User user, Integer vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe."));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Xe này không thuộc tài khoản của bạn.");
        }
        return vehicle;
    }

    private LocalTime parseStartTime(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            throw new RuntimeException("Vui lòng chọn thời gian hẹn.");
        }
        String rawStart = timeSlot.split("-")[0].trim();
        return LocalTime.parse(rawStart);
    }

}
