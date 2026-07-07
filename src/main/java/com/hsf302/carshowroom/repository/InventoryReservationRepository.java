package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.common.Enums.ReservationStatus;
import com.hsf302.carshowroom.entity.InventoryReservation;
import com.hsf302.carshowroom.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Integer> {
    List<InventoryReservation> findByOrder(Order order);

    List<InventoryReservation> findByOrderAndReservationStatusIn(Order order, List<ReservationStatus> statuses);

    List<InventoryReservation> findByReservationStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime now);
}
