package com.hsf302.carshowroom.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "email", nullable = false,columnDefinition = "NVARCHAR(155)")
    private String email;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "password_hash", nullable = false,columnDefinition = "NVARCHAR(255)")
    private String passwordHash;

    @Nationalized
    @Lob
    @Column(name = "jwt_refresh_token")
    private String jwtRefreshToken;

    @Size(max = 150)
    @NotNull
    @Nationalized
    @Column(name = "full_name", nullable = false,columnDefinition = "NVARCHAR(150)")
    private String fullName;

    @Size(max = 20)
    @Nationalized
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 255)
    @Nationalized
    @Column(name = "address",columnDefinition = "NVARCHAR(255)")
    private String address;

    @Size(max = 50)
    @NotNull
    @Nationalized
    @Column(name = "role", nullable = false,columnDefinition = "NVARCHAR(50)")
    private String role;

    @Size(max = 50)
    @NotNull
    @Nationalized
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, columnDefinition = "NVARCHAR(50)")
    private String status;


}
