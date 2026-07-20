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
@Table(name = "car_model")
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_model_id", nullable = false)
    private Integer id;

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


}
