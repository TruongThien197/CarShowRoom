package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.WorkshopClosedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface WorkshopClosedDateRepository extends JpaRepository<WorkshopClosedDate, Long> {
    boolean existsByClosedDate(LocalDate closedDate);
}
