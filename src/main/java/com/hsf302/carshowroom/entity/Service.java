package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "service")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id", nullable = false)
    private Integer id;

    @Size(max = 150)
    @NotNull
    @Nationalized
    @Column(name = "service_name", nullable = false,columnDefinition = "NVARCHAR(150)")
    private String serviceName;

    @Nationalized
    @Lob
    @Column(name = "description",columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @NotNull
    @Column(name = "min_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal minPrice;

    @NotNull
    @Column(name = "max_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal maxPrice;

    @NotNull
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceStatus status;
}
