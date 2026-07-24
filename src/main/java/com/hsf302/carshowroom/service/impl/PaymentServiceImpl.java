package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.BookingType;
import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.config.PayOSProperties;
import com.hsf302.carshowroom.dto.PayOS.PayOSCreatePaymentLinkRequest;
import com.hsf302.carshowroom.dto.PayOS.PayOSWebhookRequest;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import com.hsf302.carshowroom.service.PaymentService;
import com.hsf302.carshowroom.service.SystemSettingService;
import com.hsf302.carshowroom.service.InventoryReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final long MIN_PAYOS_AMOUNT = 2_000L;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final OrderWorkflowService orderWorkflowService;
    private final InventoryReservationService inventoryReservationService;
    private final PayOS payOS;
    private final PayOSProperties properties;
    private final SystemSettingService settingService;

    /** Tạo liên kết thanh toán trực tuyến và lưu giao dịch chờ thanh toán trong hệ thống. */
    @Override
    @Transactional
    public PaymentTransaction createPaymentLink(PayOSCreatePaymentLinkRequest request) {
        requireConfigured();
        BigDecimal paymentAmount = resolvePaymentAmount(request);
        long amount = toPayOSAmount(paymentAmount);
        long orderCode = generateOrderCode();
        long expiredAt = Instant.now().plusSeconds(
                settingService.getInt(SystemSettingServiceImpl.PAYMENT_HOLD_MINUTES) * 60L).getEpochSecond();

        CreatePaymentLinkRequest.CreatePaymentLinkRequestBuilder paymentBuilder = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description("Đơn hàng " + orderCode)
                .returnUrl(properties.returnUrl())
                .cancelUrl(properties.cancelUrl())
                .item(PaymentLinkItem.builder()
                        .name("Thanh toán GearShift")
                        .quantity(1)
                        .price(amount)
                        .unit("VND")
                        .build())
                .expiredAt(expiredAt)
                .buyerName(safePayOSText(request.getUser() == null ? null : request.getUser().getFullName(), 100))
                .buyerEmail(safePayOSText(request.getUser() == null ? null : request.getUser().getEmail(), 100))
                .buyerPhone(safePayOSText(request.getUser() == null ? null : request.getUser().getPhone(), 20))
                .buyerAddress(safePayOSText(request.getUser() == null ? null : request.getUser().getAddress(), 255));
        CreatePaymentLinkRequest paymentRequest = paymentBuilder.build();
        CreatePaymentLinkResponse response;
        try {
            response = payOS.paymentRequests().create(paymentRequest);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("PayOS từ chối tạo link thanh toán. Vui lòng kiểm tra lại cấu hình API key, checksum key và return/cancel URL.", exception);
        }
        if (response == null || !hasText(response.getCheckoutUrl())) {
            throw new IllegalStateException("PayOS không trả về đường dẫn thanh toán.");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(request.getUser());
        transaction.setParentOrder(request.getParentOrder());
        transaction.setOrder(resolveMainOrder(request));
        transaction.setBooking(request.getBooking());
        transaction.setPaymentPurpose(request.getPaymentPurpose() == null ? "DEPOSIT" : request.getPaymentPurpose());
        transaction.setAmount(paymentAmount);
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setPayosOrderCode(String.valueOf(orderCode));
        transaction.setCheckoutUrl(response.getCheckoutUrl());
        transaction.setPaymentDeadline(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(expiredAt), APP_ZONE));
        return paymentTransactionRepository.save(transaction);
    }

    /** Đồng bộ trạng thái giao dịch từ cổng thanh toán theo mã giao dịch. */
    @Override
    @Transactional
    public PaymentTransaction syncPaymentStatus(String orderCode) {
        requireConfigured();
        PaymentTransaction transaction = paymentTransactionRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch PayOS: " + orderCode));
        PaymentLink paymentLink = payOS.paymentRequests().get(Long.valueOf(orderCode));
        validateAmount(transaction, paymentLink.getAmount());
        applyStatus(transaction, paymentLink.getStatus());
        return paymentTransactionRepository.save(transaction);
    }

    /** Xác thực webhook từ cổng thanh toán và đánh dấu giao dịch đã thanh toán. */
    @Override
    @Transactional
    public void handlePayOSWebhook(PayOSWebhookRequest request) {
        requireConfigured();
        WebhookData data = payOS.webhooks().verify(request);
        if (!isSuccessfulWebhook(request)) {
            throw new IllegalArgumentException("Webhook PayOS không phải sự kiện thanh toán thành công.");
        }
        PaymentTransaction transaction = paymentTransactionRepository
                .findByPayosOrderCode(String.valueOf(data.getOrderCode()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy giao dịch PayOS: " + data.getOrderCode()));
        validateAmount(transaction, data.getAmount());
        transaction.setRawWebhookPayload(writeWebhookPayload(request));
        markPaid(transaction);
        paymentTransactionRepository.save(transaction);
    }

    /** Đánh dấu các giao dịch quá hạn và giải phóng hàng, lịch hẹn đang được giữ. */
    @Override
    @Transactional
    public void expirePendingPayments() {
        List<PaymentTransaction> expiredTransactions =
                paymentTransactionRepository.findByStatusAndPaymentDeadlineBefore(
                        PaymentStatus.PENDING, LocalDateTime.now(APP_ZONE));
        for (PaymentTransaction transaction : expiredTransactions) {
            markUnpaidTerminal(transaction, PaymentStatus.EXPIRED,
                    OrderStatus.EXPIRED_PAYMENT, BookingStatus.EXPIRED_PAYMENT);
        }
        paymentTransactionRepository.saveAll(expiredTransactions);
    }

    /** Trả về mã đơn hàng liên kết với giao dịch trực tuyến. */
    @Override
    @Transactional(readOnly = true)
    public Integer getOrderIdByPayOSCode(String orderCode) {
        PaymentTransaction transaction = paymentTransactionRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch PayOS."));
        if (transaction.getParentOrder() != null) {
            return transaction.getParentOrder().getId();
        }
        if (transaction.getOrder() != null) {
            return transaction.getOrder().getId();
        }
        throw new IllegalArgumentException("Giao dịch PayOS không có đơn hàng liên kết.");
    }

    /** Trả về mã lịch hẹn liên kết với giao dịch trực tuyến, nếu có. */
    @Override
    @Transactional(readOnly = true)
    public Integer getBookingIdByPayOSCode(String orderCode) {
        PaymentTransaction transaction = paymentTransactionRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch PayOS."));
        return transaction.getBooking() == null ? null : transaction.getBooking().getId();
    }

    /** Áp dụng trạng thái trả về từ cổng thanh toán vào giao dịch nội bộ. */
    private void applyStatus(PaymentTransaction transaction, PaymentLinkStatus status) {
        if (status == PaymentLinkStatus.PAID) {
            markPaid(transaction);
        } else if (status == PaymentLinkStatus.CANCELLED) {
            markCancelledForRetry(transaction);
        } else if (status == PaymentLinkStatus.EXPIRED) {
            markUnpaidTerminal(transaction, PaymentStatus.EXPIRED,
                    OrderStatus.EXPIRED_PAYMENT, BookingStatus.EXPIRED_PAYMENT);
        } else if (status == PaymentLinkStatus.FAILED) {
            transaction.setStatus(PaymentStatus.FAILED);
        } else {
            transaction.setStatus(PaymentStatus.PENDING);
        }
    }

    /**
     * Đồng bộ trạng thái khi PayOS báo đã thanh toán: xác nhận đơn, giữ hàng
     * và xác nhận lịch hẹn nếu giao dịch là tiền cọc lắp đặt.
     */
    /** Chuyển trạng thái thanh toán thành công sang đơn hàng, tồn kho và lịch hẹn liên quan. */
    private void markPaid(PaymentTransaction transaction) {
        if (transaction.getStatus() == PaymentStatus.PAID) {
            return;
        }
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setPaidAt(LocalDateTime.now(APP_ZONE));

        Order parent = transaction.getParentOrder();
        if (parent != null) {
            parent.setPaymentStatus(PaymentStatus.PAID);
            parent.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(parent);
            parent.getSubOrders().forEach(this::markOrderPaid);
        } else if (transaction.getOrder() != null) {
            markOrderPaid(transaction.getOrder());
        }

        Booking booking = transaction.getBooking();
        if (booking != null) {
            if ("REMAINING".equalsIgnoreCase(transaction.getPaymentPurpose())) {
                booking.setRemainingPaymentStatus(PaymentStatus.PAID);
            } else {
                booking.setPaymentStatus(PaymentStatus.PAID);
                booking.setBookingStatus(booking.getBookingType() == BookingType.PART_INSTALLATION
                        ? BookingStatus.CONFIRMED
                        : BookingStatus.WAITING_FOR_VEHICLE);
            }
            bookingRepository.save(booking);
        }
    }

    /** Đánh dấu một đơn đã thanh toán và chuyển đơn sang trạng thái xử lý. */
    private void markOrderPaid(Order order) {
        order.setPaymentStatus(PaymentStatus.PAID);
        orderWorkflowService.processOrder(order);
    }

    /** Xử lý trạng thái thanh toán kết thúc nhưng không thành công cho đơn và lịch hẹn liên quan. */
    private void markUnpaidTerminal(PaymentTransaction transaction, PaymentStatus paymentStatus,
                                    OrderStatus orderStatus, BookingStatus bookingStatus) {
        if (transaction.getStatus() == PaymentStatus.PAID) {
            return;
        }
        transaction.setStatus(paymentStatus);
        Order parent = transaction.getParentOrder();
        if (parent != null) {
            parent.setPaymentStatus(paymentStatus);
            parent.setOrderStatus(orderStatus);
            orderRepository.save(parent);
            parent.getSubOrders().forEach(order ->
                    markOrderUnpaidTerminal(order, paymentStatus, orderStatus));
        } else if (transaction.getOrder() != null) {
            markOrderUnpaidTerminal(transaction.getOrder(), paymentStatus, orderStatus);
        }
        Booking booking = transaction.getBooking();
        if (booking != null) {
            if ("REMAINING".equalsIgnoreCase(transaction.getPaymentPurpose())) {
                booking.setRemainingPaymentStatus(paymentStatus);
            } else {
                booking.setPaymentStatus(paymentStatus);
                booking.setBookingStatus(bookingStatus);
            }
            bookingRepository.save(booking);
        }
    }

    /** Trả hàng đang giữ và cập nhật một đơn sang trạng thái thanh toán không thành công. */
    private void markOrderUnpaidTerminal(Order order, PaymentStatus paymentStatus,
                                         OrderStatus orderStatus) {
        inventoryReservationService.releaseReservation(order);
        order.setPaymentStatus(paymentStatus);
        order.setOrderStatus(orderStatus);
        orderRepository.save(order);
    }

    /** Hủy giao dịch chưa thanh toán để khách có thể tạo một lần thanh toán mới. */
    private void markCancelledForRetry(PaymentTransaction transaction) {
        if (transaction.getStatus() == PaymentStatus.PAID) {
            return;
        }
        transaction.setStatus(PaymentStatus.CANCELED);
        Order parent = transaction.getParentOrder();
        if (parent != null) {
            parent.setPaymentStatus(PaymentStatus.PENDING);
            parent.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(parent);
            parent.getSubOrders().forEach(this::resetOrderForPaymentRetry);
        } else if (transaction.getOrder() != null) {
            resetOrderForPaymentRetry(transaction.getOrder());
        }
        if (transaction.getBooking() != null) {
            Booking booking = transaction.getBooking();
            if ("REMAINING".equalsIgnoreCase(transaction.getPaymentPurpose())) {
                booking.setRemainingPaymentStatus(PaymentStatus.PENDING);
            } else {
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
            }
            bookingRepository.save(booking);
        }
    }

    /** Đặt lại đơn về chờ thanh toán và giải phóng giữ hàng trước khi thanh toán lại. */
    private void resetOrderForPaymentRetry(Order order) {
        inventoryReservationService.releaseReservation(order);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);
    }

    /** Đối chiếu số tiền từ cổng thanh toán với số tiền giao dịch đã lưu. */
    private void validateAmount(PaymentTransaction transaction, Long remoteAmount) {
        if (remoteAmount == null || transaction.getAmount().compareTo(BigDecimal.valueOf(remoteAmount)) != 0) {
            throw new IllegalStateException("Số tiền PayOS không khớp với giao dịch trong hệ thống.");
        }
    }

    /** Chuyển số tiền sang số nguyên VNĐ và kiểm tra ngưỡng thanh toán tối thiểu. */
    private long toPayOSAmount(BigDecimal amount) {
        if (amount == null || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Số tiền PayOS phải là số nguyên VND.");
        }
        long value = amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        if (value < MIN_PAYOS_AMOUNT) {
            throw new IllegalArgumentException("PayOS yêu cầu số tiền thanh toán tối thiểu là 2.000 VND.");
        }
        return value;
    }

    /** Sinh mã giao dịch dạng số để gửi sang cổng thanh toán. */
    private long generateOrderCode() {
        return System.currentTimeMillis() * 100
                + ThreadLocalRandom.current().nextInt(10, 100);
    }

    /** Kiểm tra thông tin xác thực và URL trả về của cổng thanh toán đã được cấu hình. */
    private void requireConfigured() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException("Thiếu PAYOS_CLIENT_ID, PAYOS_API_KEY hoặc PAYOS_CHECKSUM_KEY.");
        }
        if (!hasText(properties.returnUrl()) || !hasText(properties.cancelUrl())) {
            throw new IllegalStateException("Thiếu PAYOS_RETURN_URL hoặc PAYOS_CANCEL_URL.");
        }
    }

    /** Xác định đơn chính cần gắn vào giao dịch khi đơn có hoặc không có đơn cha. */
    private Order resolveMainOrder(PayOSCreatePaymentLinkRequest request) {
        if (request.getParentOrder() != null) {
            return null;
        }
        List<Order> subOrders = request.getSubOrders();
        return subOrders == null || subOrders.isEmpty() ? null : subOrders.get(0);
    }

    /**
     * Xác định số tiền gửi PayOS. Có lịch hẹn nghĩa là chỉ thu tiền cọc;
     * đơn giao hàng thông thường mới thu toàn bộ tiền phụ tùng.
     */
    private BigDecimal resolvePaymentAmount(PayOSCreatePaymentLinkRequest request) {
        if (request.getBooking() != null) {
            // Workshop orders collect only the booking deposit online.
            // The parts and installation balance are settled at the workshop.
            return resolveBookingDeposit(request.getBooking());
        }
        if (request.getParentOrder() != null) {
            BigDecimal amount = request.getParentOrder().getProductTotal();
            return amount;
        }
        BigDecimal amount = request.getSubOrders() == null ? BigDecimal.ZERO
                : request.getSubOrders().stream()
                .map(order -> order.getProductTotal().add(order.getShippingFee() == null
                        ? BigDecimal.ZERO : order.getShippingFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return amount;
    }

    /** Tính lại tiền cọc từ giá tối thiểu khi bản ghi lịch hẹn chưa có tiền cọc. */
    private BigDecimal resolveBookingDeposit(Booking booking) {
        if (booking.getBookingType() == BookingType.PART_INSTALLATION) {
            return BigDecimal.ZERO;
        }
        if (booking.getDepositAmount() != null && booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            return booking.getDepositAmount();
        }
        BigDecimal estimatedMin = booking.getEstimatedMinAmount() == null
                ? BigDecimal.ZERO : booking.getEstimatedMinAmount();
        return estimatedMin.multiply(BigDecimal.valueOf(0.20))
                .max(BigDecimal.valueOf(2_000))
                .min(BigDecimal.valueOf(10_000))
                .setScale(0, RoundingMode.UP);
    }

    /** Chuẩn hóa và giới hạn độ dài dữ liệu văn bản trước khi gửi sang cổng thanh toán. */
    private String safePayOSText(String value, int maxLength) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    /** Kiểm tra chuỗi có nội dung sau khi loại bỏ khoảng trắng hay không. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isSuccessfulWebhook(PayOSWebhookRequest request) { return request != null; }

    private String writeWebhookPayload(PayOSWebhookRequest request) { return String.valueOf(request); }
}
