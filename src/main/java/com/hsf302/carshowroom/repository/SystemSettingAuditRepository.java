package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.SystemSettingAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemSettingAuditRepository extends JpaRepository<SystemSettingAudit, Long> {
    List<SystemSettingAudit> findTop20ByOrderByUpdatedAtDesc();
}
