package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    boolean existsByReferenceId(String referenceId);

    Optional<RefundTransaction> findByReferenceId(String referenceId);

    List<RefundTransaction> findByOrderOrderByCreatedAtDesc(Order order);

    List<RefundTransaction> findByBookingOrderByCreatedAtDesc(Booking booking);
}
