package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
