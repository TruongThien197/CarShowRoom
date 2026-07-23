package com.hsf302.carshowroom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_setting_audits")
@Getter
@Setter
@NoArgsConstructor
public class SystemSettingAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;
    @Column(name = "old_value", nullable = false, length = 255)
    private String oldValue;
    @Column(name = "new_value", nullable = false, length = 255)
    private String newValue;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;
    @CreationTimestamp
    @Column(name = "updated_at", updatable = false)
    private LocalDateTime updatedAt;
}
