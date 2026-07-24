package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.RefundTransaction;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.repository.RefundTransactionRepository;
import com.hsf302.carshowroom.service.RefundService;
import com.hsf302.carshowroom.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private final RefundTransactionRepository refundRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final SystemSettingService settingService;

    /** Tạo yêu cầu hoàn toàn bộ khoản đã thanh toán của một đơn hàng. */
    @Override
    @Transactional
    public RefundTransaction requestOrderRefund(Order order, String reason) {
        if (refundRepository.existsByOrderAndStatusIn(order, List.of(RefundStatus.REQUESTED, RefundStatus.COMPLETED))) {
            throw new IllegalStateException("Đơn hàng này đã có yêu cầu hoàn tiền.");
        }
        PaymentTransaction payment = paymentRepository.findByOrderOrParentOrder(order, order).stream()
                .filter(transaction -> transaction.getStatus() == PaymentStatus.PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giao dịch đã thanh toán để hoàn tiền."));
        return create(payment, order, null, payment.getAmount(), reason);
    }

    /** Tạo yêu cầu hoàn tiền cọc của lịch hẹn với số tiền hợp lệ. */
    @Override
    @Transactional
    public RefundTransaction requestBookingRefund(Booking booking, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException("Lịch hẹn này không thuộc trường hợp được hoàn tiền.");
        }
        if (refundRepository.existsByBookingAndStatusIn(booking, List.of(RefundStatus.REQUESTED, RefundStatus.COMPLETED))) {
            throw new IllegalStateException("Lịch hẹn này đã có yêu cầu hoàn tiền.");
        }
        PaymentTransaction payment = paymentRepository.findByBooking(booking).stream()
                .filter(transaction -> transaction.getStatus() == PaymentStatus.PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giao dịch đã thanh toán để hoàn tiền."));
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được lớn hơn số tiền khách đã thanh toán.");
        }
        return create(payment, null, booking, amount, reason);
    }

    /** Đánh dấu yêu cầu hoàn tiền đã hoàn tất và cập nhật trạng thái thanh toán liên quan. */
    @Override
    @Transactional
    public void complete(Long refundId, User processedBy, String note) {
        RefundTransaction refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền."));
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Yêu cầu hoàn tiền này không còn chờ xử lý.");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập ghi chú hoặc mã giao dịch hoàn tiền.");
        }
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refund.setProcessedBy(processedBy);
        refund.setNote(note.trim());
        if (refund.getOrder() != null) {
            refund.getOrder().setRefundStatus(RefundStatus.COMPLETED);
            refund.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
            refund.getOrder().setRefundNote(note.trim());
            refund.getOrder().setRefundedAt(LocalDateTime.now());
            refund.getOrder().setRefundedBy(processedBy);
        }
        if (refund.getBooking() != null) {
            refund.getBooking().setRefundStatus(RefundStatus.COMPLETED);
            refund.getBooking().setPaymentStatus(PaymentStatus.REFUNDED);
            refund.getBooking().setRefundNote(note.trim());
            refund.getBooking().setRefundedAt(LocalDateTime.now());
            refund.getBooking().setRefundedBy(processedBy);
        }
    }

    /** Lấy lịch sử các yêu cầu hoàn tiền của một đơn hàng. */
    @Override public List<RefundTransaction> getOrderRefunds(Order order) { return refundRepository.findByOrderOrderByRequestedAtDesc(order); }

    /** Lấy lịch sử các yêu cầu hoàn tiền của một lịch hẹn. */
    @Override public List<RefundTransaction> getBookingRefunds(Booking booking) { return refundRepository.findByBookingOrderByRequestedAtDesc(booking); }

    /** Khởi tạo bản ghi hoàn tiền, gán thời hạn xử lý theo cấu hình và lưu yêu cầu. */
    private RefundTransaction create(PaymentTransaction payment, Order order, Booking booking, BigDecimal amount, String reason) {
        RefundTransaction refund = new RefundTransaction();
        refund.setPaymentTransaction(payment);
        refund.setOrder(order);
        refund.setBooking(booking);
        refund.setRefundAmount(amount);
        refund.setReason(reason == null || reason.isBlank() ? "Khách hàng yêu cầu hủy." : reason.trim());
        refund.setStatus(RefundStatus.REQUESTED);
        refund.setRefundDeadline(LocalDateTime.now().plusHours(
                settingService.getInt(SystemSettingServiceImpl.REFUND_SLA_HOURS)));
        if (order != null) order.setRefundStatus(RefundStatus.REQUESTED);
        if (booking != null) booking.setRefundStatus(RefundStatus.REQUESTED);
        return refundRepository.save(refund);
    }
}
