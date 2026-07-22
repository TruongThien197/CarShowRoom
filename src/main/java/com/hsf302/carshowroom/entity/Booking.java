package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_order_id")
    private Order relatedOrder;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "time_slot")
    private String timeSlot;

    @Column(name = "total_duration_minutes", nullable = false)
    private Integer totalDurationMinutes;

    @Column(name = "estimated_min_amount", nullable = false)
    private BigDecimal estimatedMinAmount = BigDecimal.ZERO;

    @Column(name = "estimated_max_amount", nullable = false)
    private BigDecimal estimatedMaxAmount = BigDecimal.ZERO;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    // Nullable keeps schema updates safe for bookings created before deposits were introduced.
    @Column(name = "deposit_amount")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status")
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "refund_note", length = 500)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "remaining_payment_status")
    private PaymentStatus remainingPaymentStatus = PaymentStatus.PENDING;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "no_show_at")
    private LocalDateTime noShowAt;

    @Lob
    private String notes;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingService> bookingServices = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingExtraItem> bookingExtraItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
