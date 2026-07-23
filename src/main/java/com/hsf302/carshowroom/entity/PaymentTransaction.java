package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    private Order parentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "payos_order_code", unique = true, nullable = false,columnDefinition = "NVARCHAR(MAX)")
    private String payosOrderCode;

    @Column(name = "payment_purpose",columnDefinition = "NVARCHAR(30)")
    private String paymentPurpose = "DEPOSIT";

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "checkout_url",columnDefinition = "NVARCHAR(MAX)")
    private String checkoutUrl;

    @Column(name = "payment_deadline", nullable = false)
    private LocalDateTime paymentDeadline;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Lob
    @Column(name = "raw_webhook_payload",columnDefinition = "NVARCHAR(MAX)")
    private String rawWebhookPayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
