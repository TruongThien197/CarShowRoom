package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.SystemSetting;
import com.hsf302.carshowroom.entity.SystemSettingAudit;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.SystemSettingRepository;
import com.hsf302.carshowroom.repository.SystemSettingAuditRepository;
import com.hsf302.carshowroom.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {
    public static final String WORK_START_TIME = "WORK_START_TIME";
    public static final String WORK_END_TIME = "WORK_END_TIME";
    public static final String LUNCH_START_TIME = "LUNCH_START_TIME";
    public static final String LUNCH_END_TIME = "LUNCH_END_TIME";
    public static final String WORKSHOP_CAPACITY = "WORKSHOP_CAPACITY";
    public static final String SLOT_STEP_MINUTES = "SLOT_STEP_MINUTES";
    public static final String MIN_BOOKING_LEAD_MINUTES = "MIN_BOOKING_LEAD_MINUTES";
    public static final String PAYMENT_HOLD_MINUTES = "PAYMENT_HOLD_MINUTES";
    public static final String NO_SHOW_GRACE_MINUTES = "NO_SHOW_GRACE_MINUTES";
    public static final String REFUND_SLA_HOURS = "REFUND_SLA_HOURS";
    public static final String CANCELLATION_FREE_HOURS = "CANCELLATION_FREE_HOURS";
    public static final String LATE_CANCEL_REFUND_PERCENT = "LATE_CANCEL_REFUND_PERCENT";
    public static final String NO_SHOW_REFUND_PERCENT = "NO_SHOW_REFUND_PERCENT";
    public static final String DEPOSIT_RATE_PERCENT = "DEPOSIT_RATE_PERCENT";
    public static final String MIN_DEPOSIT_AMOUNT = "MIN_DEPOSIT_AMOUNT";
    public static final String MAX_DEPOSIT_AMOUNT = "MAX_DEPOSIT_AMOUNT";

    private final SystemSettingRepository repository;
    private final SystemSettingAuditRepository auditRepository;

    /** Lấy toàn bộ cấu hình sau khi bảo đảm các giá trị mặc định đã tồn tại. */
    @Override
    @Transactional
    public List<SystemSetting> getAll() {
        ensureDefaults();
        return repository.findAll().stream().sorted(java.util.Comparator.comparing(SystemSetting::getKey)).toList();
    }

    /** Trả về cấu hình dưới dạng cặp khóa/giá trị để các dịch vụ nghiệp vụ sử dụng. */
    @Override
    @Transactional
    public Map<String, String> getValues() {
        ensureDefaults();
        return repository.findAll().stream().collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));
    }

    /** Đọc một cấu hình số nguyên và báo lỗi nếu giá trị lưu không hợp lệ. */
    @Override
    public int getInt(String key) {
        try {
            return Integer.parseInt(getValues().get(key));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cấu hình " + key + " không hợp lệ.");
        }
    }

    /** Đọc một cấu hình giờ theo định dạng thời gian và báo lỗi nếu không hợp lệ. */
    @Override
    public LocalTime getTime(String key) {
        try {
            return LocalTime.parse(getValues().get(key));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cấu hình " + key + " không hợp lệ.");
        }
    }

    /** Cập nhật cấu hình đã kiểm tra hợp lệ và lưu lịch sử cho từng giá trị thay đổi. */
    @Override
    @Transactional
    public void update(Map<String, String> values, User updatedBy) {
        ensureDefaults();
        Map<String, String> merged = new LinkedHashMap<>(getValues());
        merged.putAll(values);
        validate(merged);
        for (SystemSetting setting : repository.findAll()) {
            String value = merged.get(setting.getKey());
            if (value != null && !setting.getValue().equals(value.trim())) {
                SystemSettingAudit audit = new SystemSettingAudit();
                audit.setSettingKey(setting.getKey());
                audit.setOldValue(setting.getValue());
                audit.setNewValue(value.trim());
                audit.setUpdatedBy(updatedBy);
                auditRepository.save(audit);
                setting.setValue(value.trim());
            }
        }
    }

    /**
     * Bổ sung cấu hình còn thiếu và sửa nhãn mô tả cũ để giao diện luôn hiển thị tiếng Việt đúng dấu.
     */
    private void ensureDefaults() {
        defaults().forEach((key, expected) -> repository.findById(key)
                .map(existing -> {
                    // Repair labels created by older versions with an incorrect character encoding.
                    if (!expected.getDescription().equals(existing.getDescription())) {
                        existing.setDescription(expected.getDescription());
                    }
                    return existing;
                })
                .orElseGet(() -> repository.save(expected)));
    }

    /** Tạo bản đồ các cấu hình chuẩn cùng giá trị mặc định và nhãn hiển thị. */
    private Map<String, SystemSetting> defaults() {
        Map<String, SystemSetting> values = new LinkedHashMap<>();
        values.put(WORK_START_TIME, new SystemSetting(WORK_START_TIME, "08:00", "Giờ bắt đầu làm việc của xưởng"));
        values.put(WORK_END_TIME, new SystemSetting(WORK_END_TIME, "17:00", "Giờ kết thúc làm việc của xưởng"));
        values.put(LUNCH_START_TIME, new SystemSetting(LUNCH_START_TIME, "12:00", "Giờ bắt đầu nghỉ trưa"));
        values.put(LUNCH_END_TIME, new SystemSetting(LUNCH_END_TIME, "13:00", "Giờ kết thúc nghỉ trưa"));
        values.put(WORKSHOP_CAPACITY, new SystemSetting(WORKSHOP_CAPACITY, "2", "Số xe tối đa xưởng có thể phục vụ cùng lúc"));
        values.put(SLOT_STEP_MINUTES, new SystemSetting(SLOT_STEP_MINUTES, "30", "Bước tạo khung giờ (phút)"));
        values.put(MIN_BOOKING_LEAD_MINUTES, new SystemSetting(MIN_BOOKING_LEAD_MINUTES, "60", "Thời gian đặt lịch trước tối thiểu (phút)"));
        values.put(PAYMENT_HOLD_MINUTES, new SystemSetting(PAYMENT_HOLD_MINUTES, "15", "Thời gian giữ hàng và slot chờ thanh toán (phút)"));
        values.put(NO_SHOW_GRACE_MINUTES, new SystemSetting(NO_SHOW_GRACE_MINUTES, "30", "Thời gian chờ khách trước khi tính vắng mặt (phút)"));
        values.put(REFUND_SLA_HOURS, new SystemSetting(REFUND_SLA_HOURS, "24", "Thời hạn xử lý hoàn tiền (giờ)"));
        values.put(CANCELLATION_FREE_HOURS, new SystemSetting(CANCELLATION_FREE_HOURS, "24", "Hủy lịch trước số giờ này được hoàn toàn bộ cọc"));
        values.put(LATE_CANCEL_REFUND_PERCENT, new SystemSetting(LATE_CANCEL_REFUND_PERCENT, "50", "Tỷ lệ hoàn cọc khi hủy sát giờ (%)"));
        values.put(NO_SHOW_REFUND_PERCENT, new SystemSetting(NO_SHOW_REFUND_PERCENT, "0", "Tỷ lệ hoàn cọc khi khách vắng mặt (%)"));
        values.put(DEPOSIT_RATE_PERCENT, new SystemSetting(DEPOSIT_RATE_PERCENT, "20", "Tỷ lệ tiền cọc dịch vụ (%)"));
        values.put(MIN_DEPOSIT_AMOUNT, new SystemSetting(MIN_DEPOSIT_AMOUNT, "2000", "Tiền cọc tối thiểu (VNĐ)"));
        values.put(MAX_DEPOSIT_AMOUNT, new SystemSetting(MAX_DEPOSIT_AMOUNT, "10000", "Tiền cọc tối đa (VNĐ)"));
        return values;
    }

    /** Kiểm tra tính hợp lệ của giờ làm việc, mức cọc, hoàn tiền và các giới hạn vận hành. */
    private void validate(Map<String, String> values) {
        LocalTime start = LocalTime.parse(values.get(WORK_START_TIME));
        LocalTime end = LocalTime.parse(values.get(WORK_END_TIME));
        LocalTime lunchStart = LocalTime.parse(values.get(LUNCH_START_TIME));
        LocalTime lunchEnd = LocalTime.parse(values.get(LUNCH_END_TIME));
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ mở cửa phải sớm hơn giờ đóng cửa.");
        }
        if (!lunchStart.isBefore(lunchEnd) || lunchStart.isBefore(start) || lunchEnd.isAfter(end)) {
            throw new IllegalArgumentException("Giờ nghỉ trưa phải nằm trong giờ làm việc và có thời điểm bắt đầu trước kết thúc.");
        }
        for (String key : List.of(WORKSHOP_CAPACITY, SLOT_STEP_MINUTES, MIN_BOOKING_LEAD_MINUTES,
                PAYMENT_HOLD_MINUTES, NO_SHOW_GRACE_MINUTES, REFUND_SLA_HOURS, CANCELLATION_FREE_HOURS)) {
            if (Integer.parseInt(values.get(key)) <= 0) {
                throw new IllegalArgumentException("Giá trị cấu hình phải lớn hơn 0.");
            }
        }
        for (String key : List.of(LATE_CANCEL_REFUND_PERCENT, NO_SHOW_REFUND_PERCENT, DEPOSIT_RATE_PERCENT)) {
            int percent = Integer.parseInt(values.get(key));
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("Tỷ lệ hoàn tiền phải nằm trong khoảng từ 0 đến 100.");
            }
        }
        int minDeposit = Integer.parseInt(values.get(MIN_DEPOSIT_AMOUNT));
        int maxDeposit = Integer.parseInt(values.get(MAX_DEPOSIT_AMOUNT));
        if (minDeposit <= 0 || maxDeposit < minDeposit) {
            throw new IllegalArgumentException("Tiền cọc tối thiểu phải lớn hơn 0 và không vượt tiền cọc tối đa.");
        }
    }
}
