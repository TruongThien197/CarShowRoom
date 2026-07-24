package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.RefundPayoutStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class RefundTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column(name = "reference_id", nullable = false, unique = true,columnDefinition = "NVARCHAR(80)")
    private String referenceId;

    @Column(name = "provider_payout_id",columnDefinition = "NVARCHAR(100)")
    private String providerPayoutId;

    @Column(name = "provider_transaction_id",columnDefinition = "NVARCHAR(100)")
    private String providerTransactionId;

    @Column(name = "provider", nullable = false,columnDefinition = "NVARCHAR(30)")
    private String provider;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "bank_name",columnDefinition = "NVARCHAR(100)")
    private String bankName;

    @Column(name = "bank_bin",columnDefinition = "NVARCHAR(20)")
    private String bankBin;

    @Column(name = "account_holder",columnDefinition = "NVARCHAR(150)")
    private String accountHolder;

    @Column(name = "account_number",columnDefinition = "NVARCHAR(50)")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,columnDefinition = "NVARCHAR(30)")
    private RefundPayoutStatus status;

    @Column(name = "note",columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "raw_response",columnDefinition = "NVARCHAR(1000)")
    private String rawResponse;

    @Column(name = "error_message",columnDefinition = "NVARCHAR(500)")
    private String errorMessage;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
