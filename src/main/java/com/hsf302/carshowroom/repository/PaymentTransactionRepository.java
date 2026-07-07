package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByPayosOrderCode(String payosOrderCode);

    List<PaymentTransaction> findByStatusAndPaymentDeadlineBefore(PaymentStatus status, LocalDateTime deadline);
}
