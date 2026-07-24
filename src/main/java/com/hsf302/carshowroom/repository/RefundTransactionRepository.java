package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.common.Enums.RefundStatus;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    List<RefundTransaction> findByOrderOrderByRequestedAtDesc(Order order);
    List<RefundTransaction> findByBookingOrderByRequestedAtDesc(Booking booking);
    boolean existsByOrderAndStatusIn(Order order, List<RefundStatus> statuses);
    boolean existsByBookingAndStatusIn(Booking booking, List<RefundStatus> statuses);
    List<RefundTransaction> findByStatusOrderByRefundDeadlineAsc(RefundStatus status);
    boolean existsByReferenceId(String referenceId);
}
