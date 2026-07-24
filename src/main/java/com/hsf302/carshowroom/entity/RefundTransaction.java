package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.RefundPayoutStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions")
@Getter
@Setter
@NoArgsConstructor
public class RefundTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private PaymentTransaction paymentTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "refund_deadline", nullable = false)
    private LocalDateTime refundDeadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column(name = "note", length = 500)
    private String note;

    /** Mã tham chiếu duy nhất để payout PayOS có thể chạy lại an toàn. */
    @Column(name = "reference_id", unique = true, length = 100)
    private String referenceId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_payout_id", length = 100)
    private String providerPayoutId;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_bin", length = 30)
    private String bankBin;

    @Column(name = "account_holder", length = 150)
    private String accountHolder;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    /** Trạng thái thực thi payout, tách biệt với trạng thái nghiệp vụ hoàn tiền. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", length = 30)
    private RefundPayoutStatus payoutStatus;

    @Column(name = "raw_response", length = 5000)
    private String rawResponse;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
