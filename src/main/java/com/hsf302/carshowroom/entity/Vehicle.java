package com.hsf302.carshowroom.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "vehicle")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id")
    private CarModel carModel;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @NotNull
    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Size(max = 30)
    @NotNull
    @Nationalized
    @Column(name = "license_plate", nullable = false, length = 30)
    private String licensePlate;

    @Transient
    public String getDisplayName() {
        return brand + " " + modelName + " " + year;
    }

}
