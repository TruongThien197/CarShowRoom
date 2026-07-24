package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.BookingType;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.RefundPayoutStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.dto.BookingForm;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.BookingService;
import com.hsf302.carshowroom.entity.RefundTransaction;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.service.RefundPayoutService;
import com.hsf302.carshowroom.service.SchedulingService;
import com.hsf302.carshowroom.service.SystemSettingService;
import com.hsf302.carshowroom.service.RefundService;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
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
    private final SystemSettingService settingService;
    private final RefundService refundService;
    private final OrderWorkflowService orderWorkflowService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundPayoutService refundPayoutService;

    /** Tạo lịch hẹn mới, tính tiền cọc, giữ khung giờ và lưu snapshot dịch vụ. */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
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
        booking.setFinalAmount(null);
        booking.setDepositAmount(calculateDeposit(selectedService.getMinPrice()));
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setBookingType(BookingType.REPAIR_SERVICE);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(
                settingService.getInt(SystemSettingServiceImpl.PAYMENT_HOLD_MINUTES)));
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

    /** Tính tiền cọc từ giá tối thiểu của dịch vụ theo tỷ lệ, mức sàn và mức trần cấu hình. */
    private BigDecimal calculateDeposit(BigDecimal estimatedMinAmount) {
        BigDecimal minimumDeposit = BigDecimal.valueOf(settingService.getInt(SystemSettingServiceImpl.MIN_DEPOSIT_AMOUNT));
        if (estimatedMinAmount == null) {
            return minimumDeposit;
        }
        return estimatedMinAmount.multiply(BigDecimal.valueOf(
                        settingService.getInt(SystemSettingServiceImpl.DEPOSIT_RATE_PERCENT)))
                .movePointLeft(2)
                .max(minimumDeposit)
                .min(BigDecimal.valueOf(settingService.getInt(SystemSettingServiceImpl.MAX_DEPOSIT_AMOUNT)))
                .setScale(0, java.math.RoundingMode.UP);
    }

    /** Lấy các lịch hẹn của một khách hàng theo ngày giảm dần. */
    @Override
    public List<Booking> getBookings(User user) {
        return bookingRepository.findByUserOrderByBookingDateDesc(user);
    }

    /** Lấy toàn bộ lịch hẹn cho màn hình quản trị. */
    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateDesc();
    }

    /** Lấy chi tiết lịch hẹn và kiểm tra lịch đó thuộc khách hàng đang xem. */
    @Override
    public Booking getBookingDetail(User user, Integer bookingId) {
        Booking booking = getBookingDetail(bookingId);
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Lịch hẹn này không thuộc tài khoản của bạn.");
        }
        return booking;
    }

    /** Lấy chi tiết lịch hẹn theo mã cho các luồng nội bộ và quản trị. */
    @Override
    public Booking getBookingDetail(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));
    }

    /** Hủy lịch hẹn của khách, tạo yêu cầu hoàn cọc khi đủ điều kiện và hủy đơn phụ tùng liên quan. */
    @Override
    @Transactional
    public void cancelBooking(User user, Integer bookingId) {
        Booking booking = getBookingDetail(user, bookingId);
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                && booking.getRefundStatus() == RefundStatus.REQUESTED) {
            throw new IllegalStateException("Y\u00eau c\u1ea7u h\u1ee7y l\u1ecbch \u0111ang ch\u1edd nh\u00e2n vi\u00ean duy\u1ec7t.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                && booking.getRefundStatus() != RefundStatus.REQUESTED
                && booking.getBookingStatus() != BookingStatus.IN_PROGRESS
                && booking.getBookingStatus() != BookingStatus.COMPLETED
                && booking.getBookingStatus() != BookingStatus.CANCELED
                && booking.getBookingStatus() != BookingStatus.EXPIRED_PAYMENT
                && booking.getBookingStatus() != BookingStatus.EXPIRED_NO_SHOW) {
            booking.setCancellationRequestedAt(LocalDateTime.now());
            booking.setRefundStatus(RefundStatus.REQUESTED);
            bookingRepository.save(booking);
            return;
        }
        if (booking.getBookingStatus() == BookingStatus.IN_PROGRESS
                || booking.getBookingStatus() == BookingStatus.COMPLETED
                || booking.getBookingStatus() == BookingStatus.CANCELED
                || booking.getBookingStatus() == BookingStatus.EXPIRED_PAYMENT
                || booking.getBookingStatus() == BookingStatus.EXPIRED_NO_SHOW) {
            throw new RuntimeException("Không thể hủy lịch hẹn ở trạng thái hiện tại.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                && booking.getRefundStatus() == RefundStatus.NONE) {
            BigDecimal refundAmount = calculateCancellationRefund(booking);
            if (refundAmount.signum() > 0) {
                refundService.requestBookingRefund(booking, refundAmount,
                        buildCancellationRefundReason(booking, refundAmount));
            }
        }
        booking.setBookingStatus(BookingStatus.CANCELED);
        if (booking.getRelatedOrder() != null) {
            orderWorkflowService.cancelOrder(booking.getRelatedOrder());
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                && booking.getRefundStatus() == RefundStatus.NONE) {
            booking.setRefundStatus(RefundStatus.REQUESTED);
        }
        if (booking.getRemainingPaymentStatus() != PaymentStatus.PAID) booking.setRemainingPaymentStatus(PaymentStatus.CANCELED);
        bookingRepository.save(booking);
    }

    /** Nhân viên duyệt yêu cầu hủy lịch đã được khách gửi và cho phép xử lý hoàn cọc. */
    @Override
    @Transactional
    public void approveCancellation(Integer bookingId, User processedBy, String assessmentNote) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getRefundStatus() == RefundStatus.REQUESTED) {
            BigDecimal refundAmount = calculateCancellationRefund(booking);
            booking.setBookingStatus(BookingStatus.CANCELED);
            if (booking.getRelatedOrder() != null) {
                orderWorkflowService.cancelOrder(booking.getRelatedOrder());
            }
            if (booking.getRemainingPaymentStatus() != PaymentStatus.PAID) {
                booking.setRemainingPaymentStatus(PaymentStatus.CANCELED);
            }
            if (refundAmount.signum() > 0) {
                refundService.requestBookingRefund(booking, refundAmount,
                        buildCancellationRefundReason(booking, refundAmount));
                booking.setRefundStatus(RefundStatus.APPROVED);
            } else {
                booking.setRefundStatus(RefundStatus.NONE);
            }
            booking.setCancellationProcessedBy(processedBy);
            booking.setCancellationProcessedAt(LocalDateTime.now());
            booking.setCancellationDecisionNote(assessmentNote == null || assessmentNote.isBlank()
                    ? "\u0110\u00e3 duy\u1ec7t y\u00eau c\u1ea7u h\u1ee7y l\u1ecbch."
                    : assessmentNote.trim());
            bookingRepository.save(booking);
            return;
        }
        if (booking.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Lịch hẹn không có yêu cầu hủy đang chờ duyệt.");
        }
        booking.setRefundStatus(booking.getPaymentStatus() == PaymentStatus.PAID
                ? RefundStatus.APPROVED : RefundStatus.NONE);
        booking.setCancellationProcessedBy(processedBy);
        booking.setCancellationProcessedAt(LocalDateTime.now());
        booking.setCancellationDecisionNote(assessmentNote == null || assessmentNote.isBlank()
                ? "Đã duyệt yêu cầu hủy lịch." : assessmentNote.trim());
        bookingRepository.save(booking);
    }

    /** Nhân viên hoàn tất hoàn tiền cọc sau khi kiểm tra thông tin tài khoản và người nhận. */
    @Override
    @Transactional
    public void rejectCancellation(Integer bookingId, User processedBy, String reason) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getRefundStatus() != RefundStatus.REQUESTED) throw new IllegalStateException("Lịch hẹn không có yêu cầu hủy đang chờ duyệt.");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        booking.setRefundStatus(RefundStatus.REJECTED);
        booking.setCancellationProcessedBy(processedBy);
        booking.setCancellationProcessedAt(LocalDateTime.now());
        booking.setCancellationDecisionNote(reason.trim());
        bookingRepository.save(booking);
    }

    private boolean requiresManualCancellationAssessment(Booking booking) {
        if (booking.getBookingDate() == null || booking.getStartTime() == null) {
            return true;
        }
        LocalDateTime appointmentStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        return !LocalDateTime.now().isBefore(appointmentStart.minusHours(24));
    }

    @Override
    @Transactional
    public void completeRefund(Integer bookingId, User processedBy, String transactionCode) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getBookingStatus() != BookingStatus.CANCELED
                || booking.getRefundStatus() != RefundStatus.APPROVED
                || booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("L\u1ecbch h\u1eb9n kh\u00f4ng c\u00f3 y\u00eau c\u1ea7u ho\u00e0n ti\u1ec1n \u0111ang ch\u1edd x\u1eed l\u00fd.");
        }
        if (booking.getRefundBankName() == null || booking.getRefundBankName().isBlank()
                || booking.getRefundAccountHolder() == null || booking.getRefundAccountHolder().isBlank()
                || booking.getRefundAccountNumber() == null || booking.getRefundAccountNumber().isBlank()) {
            throw new IllegalStateException("Kh\u00e1ch h\u00e0ng ch\u01b0a cung c\u1ea5p \u0111\u1ee7 t\u00e0i kho\u1ea3n nh\u1eadn ho\u00e0n ti\u1ec1n.");
        }
        var refund = refundService.getBookingRefunds(booking).stream()
                .filter(item -> item.getStatus() == RefundStatus.REQUESTED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Kh\u00f4ng c\u00f3 y\u00eau c\u1ea7u ho\u00e0n ti\u1ec1n \u0111ang ch\u1edd x\u1eed l\u00fd."));
        if (transactionCode == null || transactionCode.isBlank()) {
            throw new IllegalArgumentException("Vui l\u00f2ng nh\u1eadp m\u00e3 giao d\u1ecbch ho\u00e0n ti\u1ec1n.");
        }
        refundService.complete(refund.getId(), processedBy, transactionCode.trim());
        bookingRepository.save(booking);
    }

    @Transactional
    public void completeRefund(Integer bookingId, User processedBy, String bankName,
                               String bankBin, String accountHolder, String accountNumber, String note) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getBookingStatus() != BookingStatus.CANCELED
                || (booking.getRefundStatus() != RefundStatus.APPROVED && booking.getRefundStatus() != RefundStatus.FAILED)
                || booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Lịch hẹn không có yêu cầu hoàn tiền đang chờ xử lý.");
        }
        if (bankName == null || bankName.isBlank()
                || bankBin == null || bankBin.isBlank()
                || accountHolder == null || accountHolder.isBlank()
                || accountNumber == null || accountNumber.isBlank()
                || note == null || note.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ ngân hàng, mã BIN, chủ tài khoản, số tài khoản và ghi chú hoàn tiền.");
        }
        if (!samePerson(accountHolder, booking.getUser().getFullName())) {
            throw new IllegalArgumentException("Tên người nhận tiền phải trùng với khách hàng đã thanh toán tiền cọc.");
        }
        booking.setRefundNote(note.trim());
        booking.setRefundBankName(bankName.trim());
        booking.setRefundBankBin(bankBin.trim());
        booking.setRefundAccountHolder(accountHolder.trim());
        booking.setRefundAccountNumber(accountNumber.trim());
        booking.setRefundedBy(processedBy);
        RefundTransaction refundTransaction = refundPayoutService.payoutBookingRefund(booking, processedBy,
                booking.getRefundBankName(), booking.getRefundBankBin(),
                booking.getRefundAccountHolder(), booking.getRefundAccountNumber(), booking.getRefundNote());
        applyBookingRefundResult(booking, refundTransaction);
        bookingRepository.save(booking);
    }

    /** Khách hàng bổ sung tài khoản nhận tiền cho yêu cầu hoàn cọc đang chờ xử lý. */
    @Override
    @Transactional
    public void submitRefundAccount(User user, Integer bookingId, String bankName,
                                    String accountHolder, String accountNumber) {
        Booking booking = getBookingDetail(user, bookingId);
        if (booking.getBookingStatus() != BookingStatus.CANCELED
                || booking.getRefundStatus() != RefundStatus.APPROVED
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

    /** So sánh hai họ tên sau khi bỏ khoảng trắng thừa, không phân biệt hoa thường. */
    private boolean samePerson(String first, String second) {
        return first.trim().replaceAll("\\s+", " ").equalsIgnoreCase(
                second == null ? "" : second.trim().replaceAll("\\s+", " "));
    }

    /** Đồng bộ trạng thái hoàn tiền của lịch hẹn theo kết quả payout từ cổng thanh toán. */
    private void applyBookingRefundResult(Booking booking, RefundTransaction refundTransaction) {
        RefundPayoutStatus payoutStatus = refundTransaction.getPayoutStatus();
        if (payoutStatus == RefundPayoutStatus.SUCCEEDED) {
            booking.setRefundStatus(RefundStatus.COMPLETED);
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            booking.setRefundedAt(refundTransaction.getRefundedAt());
        } else if (payoutStatus == RefundPayoutStatus.FAILED) {
            booking.setRefundStatus(RefundStatus.FAILED);
            booking.setRefundNote(refundTransaction.getErrorMessage());
        } else {
            booking.setRefundStatus(RefundStatus.PROCESSING);
        }
    }

    /** Tính số tiền cọc được hoàn khi hủy, dựa trên thời điểm hủy và chính sách cấu hình. */
    private BigDecimal calculateCancellationRefund(Booking booking) {
        BigDecimal deposit = booking.getDepositAmount() == null ? BigDecimal.ZERO : booking.getDepositAmount();
        LocalDateTime appointment = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        LocalDateTime freeCancellationDeadline = appointment.minusHours(
                settingService.getInt(SystemSettingServiceImpl.CANCELLATION_FREE_HOURS));
        LocalDateTime cancellationTime = booking.getCancellationRequestedAt() == null
                ? LocalDateTime.now() : booking.getCancellationRequestedAt();
        if (!cancellationTime.isAfter(freeCancellationDeadline)) {
            return deposit;
        }
        return deposit.multiply(BigDecimal.valueOf(
                settingService.getInt(SystemSettingServiceImpl.LATE_CANCEL_REFUND_PERCENT)))
                .movePointLeft(2);
    }

    /** Ghi nhận căn cứ tính hoàn cọc để nhân viên có thể đối chiếu trước khi chuyển khoản. */
    private String buildCancellationRefundReason(Booking booking, BigDecimal refundAmount) {
        int freeHours = settingService.getInt(SystemSettingServiceImpl.CANCELLATION_FREE_HOURS);
        int latePercent = settingService.getInt(SystemSettingServiceImpl.LATE_CANCEL_REFUND_PERCENT);
        BigDecimal deposit = booking.getDepositAmount() == null ? BigDecimal.ZERO : booking.getDepositAmount();
        if (refundAmount.compareTo(deposit) == 0) {
            return "Khách hủy trước ít nhất " + freeHours + " giờ, hoàn 100% tiền cọc.";
        }
        return "Khách hủy trong vòng " + freeHours + " giờ trước giờ hẹn, hoàn " + latePercent + "% tiền cọc.";
    }

    /** Cập nhật trạng thái lịch hẹn sau khi kiểm tra luồng chuyển trạng thái hợp lệ. */
    @Override
    @Transactional
    public void checkIn(Integer bookingId) {
        Booking booking = getBookingDetail(bookingId);
        assertNoPendingCancellation(booking);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED
                && booking.getBookingStatus() != BookingStatus.WAITING_FOR_VEHICLE) {
            throw new IllegalStateException("Chỉ có thể tiếp nhận xe của lịch đã xác nhận.");
        }
        booking.setBookingStatus(BookingStatus.RECEIVING_VEHICLE);
        booking.setCheckedInAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void setFinalAmount(Integer bookingId, BigDecimal finalAmount) {
        Booking booking = getBookingDetail(bookingId);
        assertNoPendingCancellation(booking);
        if (booking.getBookingType() == BookingType.PART_INSTALLATION) {
            throw new IllegalStateException("Đơn lắp đặt tại xưởng không dùng giá dịch vụ cuối. Vui lòng nhập tiền công lắp đặt.");
        }
        if (booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Chỉ có thể nhập giá cuối sau khi đã tiếp nhận xe.");
        }
        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá cuối phải lớn hơn hoặc bằng 0.");
        }
        BigDecimal deposit = booking.getDepositAmount() == null ? BigDecimal.ZERO : booking.getDepositAmount();
        if (finalAmount.compareTo(deposit) < 0) {
            throw new IllegalArgumentException("Giá cuối không được thấp hơn tiền cọc đã thanh toán.");
        }
        booking.setFinalAmount(finalAmount);
        booking.setRemainingPaymentStatus(PaymentStatus.PENDING);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void recordLaborCollection(Integer bookingId, User staff, BigDecimal laborFee) {
        Booking booking = getBookingDetail(bookingId);
        assertNoPendingCancellation(booking);
        if (booking.getBookingType() != BookingType.PART_INSTALLATION) {
            throw new IllegalStateException("Chỉ lịch lắp đặt phụ tùng tại xưởng mới dùng tiền công lắp đặt.");
        }
        if (booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Chỉ có thể thu tiền công khi lắp đặt đang được thực hiện.");
        }
        if (staff == null || staff.getId() == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên thu tiền công.");
        }
        if (laborFee == null || laborFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền công phải lớn hơn hoặc bằng 0.");
        }
        booking.setLaborFee(laborFee);
        booking.setLaborCollected(true);
        booking.setLaborCollectedAt(LocalDateTime.now());
        booking.setLaborCollectedBy(staff);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void markNoShow(Integer bookingId) {
        Booking booking = getBookingDetail(bookingId);
        assertNoPendingCancellation(booking);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED
                && booking.getBookingStatus() != BookingStatus.WAITING_FOR_VEHICLE) {
            throw new IllegalStateException("Chỉ có thể đánh dấu không đến với lịch đã xác nhận.");
        }
        LocalDateTime scheduledStart = LocalDateTime.of(booking.getBookingDate(), parseStartTime(booking.getTimeSlot()));
        if (LocalDateTime.now().isBefore(scheduledStart)) {
            throw new IllegalStateException("Chỉ có thể đánh dấu không đến sau thời điểm bắt đầu lịch hẹn.");
        }
        booking.setBookingStatus(BookingStatus.EXPIRED_NO_SHOW);
        if (booking.getRemainingPaymentStatus() != PaymentStatus.PAID) {
            booking.setRemainingPaymentStatus(PaymentStatus.CANCELED);
        }
        booking.setNoShowAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void expireDepositPaymentIfDue(Integer bookingId) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getPaymentStatus() == PaymentStatus.PAID
                || booking.getPaymentDeadline() == null
                || booking.getPaymentDeadline().isAfter(LocalDateTime.now())) {
            return;
        }
        booking.setBookingStatus(BookingStatus.EXPIRED_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.EXPIRED);
        if (booking.getRemainingPaymentStatus() != PaymentStatus.PAID) {
            booking.setRemainingPaymentStatus(PaymentStatus.CANCELED);
        }
        if (booking.getRelatedOrder() != null) {
            orderWorkflowService.cancelOrder(booking.getRelatedOrder());
        }
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void reopenDepositPayment(Integer bookingId) {
        Booking booking = getBookingDetail(bookingId);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Lịch hẹn này đã được thanh toán tiền cọc.");
        }
        if (booking.getBookingStatus() == BookingStatus.EXPIRED_PAYMENT) {
            throw new IllegalStateException("Đã hết thời gian giữ chỗ. Vui lòng tạo lịch hẹn mới.");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Lịch hẹn không còn cho phép thanh toán tiền cọc.");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Integer bookingId, String status) {
        Booking booking = getBookingDetail(bookingId);
        assertNoPendingCancellation(booking);
        BookingStatus nextStatus = BookingStatus.valueOf(status.toUpperCase());
        if (booking.getBookingStatus() == nextStatus) {
            return;
        }
        validateStatusTransition(booking, nextStatus);
        booking.setBookingStatus(nextStatus);
        bookingRepository.save(booking);
    }

    /** Ngăn thay đổi vận hành khi khách đã gửi yêu cầu hủy lịch đang chờ duyệt. */
    private void assertNoPendingCancellation(Booking booking) {
        if (booking.getRefundStatus() == RefundStatus.REQUESTED) {
            throw new IllegalStateException("Lịch hẹn đang có yêu cầu hủy chờ duyệt nên chưa thể cập nhật.");
        }
    }

    /** Bảo vệ các quy tắc chuyển trạng thái của vòng đời lịch hẹn. */
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
        if (nextStatus == BookingStatus.IN_PROGRESS && currentStatus != BookingStatus.RECEIVING_VEHICLE) {
            throw new IllegalStateException("Xe phải được tiếp nhận trước khi chuyển sang đang sửa chữa.");
        }
        if (nextStatus == BookingStatus.COMPLETED && currentStatus != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Lịch hẹn phải ở trạng thái đang thực hiện trước khi hoàn tất.");
        }
        if (nextStatus == BookingStatus.COMPLETED) {
            if (booking.getBookingType() == BookingType.PART_INSTALLATION
                    && (booking.getLaborFee() == null || !booking.isLaborCollected())) {
                throw new IllegalStateException("Cần nhập và xác nhận đã thu tiền công trước khi hoàn tất lắp đặt.");
            }
            if (booking.getBookingType() != BookingType.PART_INSTALLATION
                    && booking.getFinalAmount() == null) {
                throw new IllegalStateException("Cần nhập giá cuối trước khi chuyển lịch sang hoàn thành.");
            }
        }
        if (nextStatus != BookingStatus.IN_PROGRESS && nextStatus != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Trạng thái lịch hẹn không được phép cập nhật thủ công.");
        }
    }

    /** Lấy xe được chọn và bảo đảm xe đó thuộc đúng khách hàng đặt lịch. */
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

    /** Trích giờ bắt đầu từ chuỗi khung giờ khách chọn. */
    private LocalTime parseStartTime(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            throw new RuntimeException("Vui lòng chọn thời gian hẹn.");
        }
        String rawStart = timeSlot.split("-")[0].trim();
        return LocalTime.parse(rawStart);
    }

}
