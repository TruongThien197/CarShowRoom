package com.hsf302.carshowroom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "workshop_closed_dates", uniqueConstraints = @UniqueConstraint(columnNames = "closed_date"))
@Getter
@Setter
@NoArgsConstructor
public class WorkshopClosedDate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "closed_date", nullable = false)
    private LocalDate closedDate;
    @Column(name = "reason", length = 250)
    private String reason;
}
