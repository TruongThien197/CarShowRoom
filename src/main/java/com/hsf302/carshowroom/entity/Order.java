package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import com.hsf302.carshowroom.common.Enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    private Order parentOrder;

    @OneToMany(mappedBy = "parentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> subOrders = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "product_total", nullable = false)
    private BigDecimal productTotal = BigDecimal.ZERO;
    
    @Column(name = "shipping_address",columnDefinition = "NVARCHAR(MAX)")
    private String shippingAddress;

    @Column(name = "receiver_phone",columnDefinition = "NVARCHAR(MAX)")
    private String receiverPhone;

    @Column(name = "cancellation_reason",columnDefinition = "NVARCHAR(MAX)")
    private String cancellationReason;

    @Column(name = "shipping_carrier",columnDefinition = "NVARCHAR(MAX)")
    private String shippingCarrier;

    @Column(name = "tracking_code",columnDefinition = "NVARCHAR(MAX)")
    private String trackingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "refund_note",columnDefinition = "NVARCHAR(MAX)")
    private String refundNote;

    @Column(name = "refund_bank_name", length = 100)
    private String refundBankName;

    @Column(name = "refund_bank_bin", length = 20)
    private String refundBankBin;

    @Column(name = "refund_account_holder", length = 150)
    private String refundAccountHolder;

    @Column(name = "refund_account_number", length = 50)
    private String refundAccountNumber;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refunded_by_id")
    private User refundedBy;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
