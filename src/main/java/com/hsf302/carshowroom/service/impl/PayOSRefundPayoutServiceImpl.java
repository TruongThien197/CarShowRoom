package com.hsf302.carshowroom.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf302.carshowroom.common.Enums.RefundPayoutStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import com.hsf302.carshowroom.config.PayOSPayoutProperties;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.RefundTransaction;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.repository.RefundTransactionRepository;
import com.hsf302.carshowroom.service.RefundPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PayOSRefundPayoutServiceImpl implements RefundPayoutService {
    private static final String PROVIDER = "PAYOS_PAYOUT";

    private final RefundTransactionRepository refundTransactionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PayOSPayoutProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RefundTransaction payoutOrderRefund(Order order, User processedBy, String note) {
        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findByOrderOrParentOrder(order, order)
                .stream()
                .max(Comparator.comparing(PaymentTransaction::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        RefundTransaction refund = baseRefund(processedBy, order.getProductTotal(), note);
        refund.setOrder(order);
        refund.setPaymentTransaction(paymentTransaction);
        refund.setBankName(requireText(order.getRefundBankName(), "Vui lòng nhập ngân hàng nhận hoàn tiền."));
        refund.setBankBin(requireText(order.getRefundBankBin(), "Vui lòng nhập mã BIN ngân hàng nhận hoàn tiền."));
        refund.setAccountHolder(requireText(order.getRefundAccountHolder(), "Vui lòng nhập tên chủ tài khoản nhận hoàn tiền."));
        refund.setAccountNumber(requireText(order.getRefundAccountNumber(), "Vui lòng nhập số tài khoản nhận hoàn tiền."));
        return createLivePayout(refund, "Hoan tien don hang " + order.getId());
    }

    @Override
    @Transactional
    public RefundTransaction payoutBookingRefund(Booking booking, User processedBy, String bankName,
                                                 String bankBin, String accountHolder,
                                                 String accountNumber, String note) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByBooking(booking);
        PaymentTransaction paymentTransaction = transactions.stream()
                .filter(transaction -> !"REMAINING".equalsIgnoreCase(transaction.getPaymentPurpose()))
                .max(Comparator.comparing(PaymentTransaction::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(transactions.isEmpty() ? null : transactions.get(0));
        RefundTransaction refund = baseRefund(processedBy, booking.getDepositAmount(), note);
        refund.setBooking(booking);
        refund.setPaymentTransaction(paymentTransaction);
        refund.setBankName(requireText(bankName, "Vui lòng nhập ngân hàng nhận hoàn tiền."));
        refund.setBankBin(requireText(bankBin, "Vui lòng nhập mã BIN ngân hàng nhận hoàn tiền."));
        refund.setAccountHolder(requireText(accountHolder, "Vui lòng nhập tên chủ tài khoản nhận hoàn tiền."));
        refund.setAccountNumber(requireText(accountNumber, "Vui lòng nhập số tài khoản nhận hoàn tiền."));
        return createLivePayout(refund, "Hoan tien coc booking " + booking.getId());
    }

    private RefundTransaction createLivePayout(RefundTransaction refund, String description) {
        requireConfigured();
        PayOSPayoutRequest request = new PayOSPayoutRequest(
                refund.getReferenceId(),
                List.of("refund"),
                true,
                List.of(new PayOSPayoutItem(
                        refund.getReferenceId(),
                        toPayOSAmount(refund.getAmount()),
                        description,
                        refund.getBankBin(),
                        refund.getAccountNumber()
                ))
        );
        String rawBody = writeJson(request);
        String signature = sign(request);
        try {
            PayOSPayoutResponse response = client().post()
                    .uri("/v1/payouts")
                    .header("x-client-id", properties.clientId())
                    .header("x-api-key", properties.apiKey())
                    .header("x-idempotency-key", refund.getReferenceId())
                    .header("x-signature", signature)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(rawBody)
                    .retrieve()
                    .body(PayOSPayoutResponse.class);
            applyResponse(refund, response, writeJson(response));
        } catch (RuntimeException exception) {
            refund.setPayoutStatus(RefundPayoutStatus.FAILED);
            refund.setErrorMessage(limit(exception.getMessage(), 500));
            refund.setRawResponse(rawBody);
        }
        return refundTransactionRepository.save(refund);
    }

    @Override
    @Transactional
    public RefundTransaction syncRefundTransaction(Long refundTransactionId) {
        RefundTransaction refund = refundTransactionRepository.findById(refundTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch hoàn tiền."));
        if (refund.getProviderPayoutId() == null || refund.getProviderPayoutId().isBlank()) {
            return refund;
        }
        requireConfigured();
        try {
            PayOSPayoutResponse response = client().get()
                    .uri("/v1/payouts/{id}", refund.getProviderPayoutId())
                    .header("x-client-id", properties.clientId())
                    .header("x-api-key", properties.apiKey())
                    .retrieve()
                    .body(PayOSPayoutResponse.class);
            applyResponse(refund, response, writeJson(response));
            applyLinkedRefundStatus(refund);
        } catch (RuntimeException exception) {
            refund.setPayoutStatus(RefundPayoutStatus.FAILED);
            refund.setErrorMessage(limit(exception.getMessage(), 500));
            applyLinkedRefundStatus(refund);
        }
        return refundTransactionRepository.save(refund);
    }

    private void applyLinkedRefundStatus(RefundTransaction refund) {
        if (refund.getOrder() != null) {
            Order order = refund.getOrder();
            if (refund.getPayoutStatus() == RefundPayoutStatus.SUCCEEDED) {
                order.setRefundStatus(RefundStatus.COMPLETED);
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                order.setRefundedAt(refund.getRefundedAt() == null ? LocalDateTime.now() : refund.getRefundedAt());
                paymentTransactionRepository.findByOrderOrParentOrder(order, order)
                        .forEach(transaction -> transaction.setStatus(PaymentStatus.REFUNDED));
            } else if (refund.getPayoutStatus() == RefundPayoutStatus.FAILED) {
                order.setRefundStatus(RefundStatus.FAILED);
                order.setRefundNote(refund.getErrorMessage());
            } else {
                order.setRefundStatus(RefundStatus.PROCESSING);
            }
        }
        if (refund.getBooking() != null) {
            Booking booking = refund.getBooking();
            if (refund.getPayoutStatus() == RefundPayoutStatus.SUCCEEDED) {
                booking.setRefundStatus(RefundStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
                booking.setRefundedAt(refund.getRefundedAt() == null ? LocalDateTime.now() : refund.getRefundedAt());
                paymentTransactionRepository.findByBooking(booking)
                        .forEach(transaction -> transaction.setStatus(PaymentStatus.REFUNDED));
            } else if (refund.getPayoutStatus() == RefundPayoutStatus.FAILED) {
                booking.setRefundStatus(RefundStatus.FAILED);
                booking.setRefundNote(refund.getErrorMessage());
            } else {
                booking.setRefundStatus(RefundStatus.PROCESSING);
            }
        }
    }

    private RefundTransaction baseRefund(User processedBy, BigDecimal amount, String note) {
        RefundTransaction refund = new RefundTransaction();
        refund.setProcessedBy(processedBy);
        refund.setReferenceId(nextReferenceId());
        refund.setProvider(PROVIDER);
        refund.setAmount(amount == null ? BigDecimal.ZERO : amount);
        refund.setPayoutStatus(RefundPayoutStatus.PROCESSING);
        refund.setNote(trim(note));
        return refund;
    }

    private void applyResponse(RefundTransaction refund, PayOSPayoutResponse response, String rawResponse) {
        refund.setRawResponse(rawResponse);
        if (response == null) {
            refund.setPayoutStatus(RefundPayoutStatus.FAILED);
            refund.setErrorMessage("PayOS không trả về response.");
            return;
        }
        refund.setErrorMessage(response.desc());
        if (!"00".equals(response.code()) || response.data() == null) {
            refund.setPayoutStatus(RefundPayoutStatus.FAILED);
            return;
        }
        refund.setProviderPayoutId(response.data().id());
        PayOSPayoutTransaction transaction = firstTransaction(response.data().transactions());
        String state = transaction == null ? response.data().approvalState() : transaction.state();
        if (transaction != null) {
            refund.setProviderTransactionId(transaction.id());
        }
        if ("SUCCEEDED".equalsIgnoreCase(state)) {
            refund.setPayoutStatus(RefundPayoutStatus.SUCCEEDED);
            refund.setRefundedAt(LocalDateTime.now());
        } else if ("FAILED".equalsIgnoreCase(state) || "REJECTED".equalsIgnoreCase(state)) {
            refund.setPayoutStatus(RefundPayoutStatus.FAILED);
        } else {
            refund.setPayoutStatus(RefundPayoutStatus.PROCESSING);
        }
    }

    private PayOSPayoutTransaction firstTransaction(Map<String, PayOSPayoutTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return null;
        }
        return transactions.values().iterator().next();
    }

    private RestClient client() {
        return RestClient.builder().baseUrl(properties.resolvedBaseUrl()).build();
    }

    private void requireConfigured() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException("Thiếu PAYOS_PAYOUT_CLIENT_ID, PAYOS_PAYOUT_API_KEY hoặc PAYOS_PAYOUT_CHECKSUM_KEY.");
        }
    }

    private long toPayOSAmount(BigDecimal amount) {
        if (amount == null || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Số tiền hoàn PayOS phải là số nguyên VND.");
        }
        long value = amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        if (value <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        }
        return value;
    }

    private String sign(Object data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.checksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signaturePayload(data).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể ký request PayOS payout.", exception);
        }
    }

    private String signaturePayload(Object data) {
        Map<String, Object> values = objectMapper.convertValue(data, Map.class);
        TreeMap<String, Object> sorted = new TreeMap<>(values);
        return sorted.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(stringify(entry.getValue())))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return writeJson(canonicalize(value));
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonicalize(item)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonicalize).toList();
        }
        return value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể serialize dữ liệu PayOS payout.", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String nextReferenceId() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String referenceId = "REFUND-" + System.currentTimeMillis()
                    + "-" + ThreadLocalRandom.current().nextInt(1000, 10_000);
            if (!refundTransactionRepository.existsByReferenceId(referenceId)) {
                return referenceId;
            }
        }
        throw new IllegalStateException("Không thể tạo mã hoàn tiền duy nhất.");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record PayOSPayoutRequest(String referenceId, List<String> category,
                                      boolean validateDestination, List<PayOSPayoutItem> payouts) {
    }

    private record PayOSPayoutItem(String referenceId, long amount, String description,
                                   String toBin, String toAccountNumber) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PayOSPayoutResponse(String code, String desc, PayOSPayoutData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PayOSPayoutData(String id, String referenceId,
                                   Map<String, PayOSPayoutTransaction> transactions,
                                   String approvalState) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PayOSPayoutTransaction(String id, String referenceId, Long amount,
                                          String description, String toBin,
                                          String toAccountNumber, String toAccountName,
                                          String state) {
    }
}
