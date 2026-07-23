package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.SystemSetting;
import com.hsf302.carshowroom.entity.User;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface SystemSettingService {
    List<SystemSetting> getAll();
    Map<String, String> getValues();
    int getInt(String key);
    LocalTime getTime(String key);
    void update(Map<String, String> values, User updatedBy);
}
