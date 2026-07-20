package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
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
    
    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "shipping_carrier")
    private String shippingCarrier;

    @Column(name = "tracking_code")
    private String trackingCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
