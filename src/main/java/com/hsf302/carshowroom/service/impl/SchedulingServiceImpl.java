package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.dto.AvailableSlotDTO;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.WorkshopClosedDateRepository;
import com.hsf302.carshowroom.service.SchedulingService;
import com.hsf302.carshowroom.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService {
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final SystemSettingService settingService;
    private final WorkshopClosedDateRepository closedDateRepository;

    /** Tạo các khung giờ còn trống theo ngày, dịch vụ, giờ làm việc, giờ nghỉ và sức chứa xưởng. */
    @Override
    public List<AvailableSlotDTO> findAvailableSlots(LocalDate date, List<Integer> serviceIds) {
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày đặt lịch không được nằm trong quá khứ.");
        }
        if (closedDateRepository.existsByClosedDate(date)) {
            throw new IllegalArgumentException("Xưởng nghỉ vào ngày đã chọn. Vui lòng chọn ngày khác.");
        }
        int duration = resolveDuration(serviceIds);
        LocalTime workStart = settingService.getTime(SystemSettingServiceImpl.WORK_START_TIME);
        LocalTime workEnd = settingService.getTime(SystemSettingServiceImpl.WORK_END_TIME);
        int slotStepMinutes = settingService.getInt(SystemSettingServiceImpl.SLOT_STEP_MINUTES);
        List<AvailableSlotDTO> slots = new ArrayList<>();
        for (LocalTime start = workStart; !start.plusMinutes(duration).isAfter(workEnd); start = start.plusMinutes(slotStepMinutes)) {
            Booking probe = new Booking();
            probe.setBookingDate(date);
            probe.setStartTime(start);
            probe.setEndTime(start.plusMinutes(duration));
            boolean meetsLeadTime = !LocalDateTime.of(date, start).isBefore(LocalDateTime.now().plusMinutes(
                    settingService.getInt(SystemSettingServiceImpl.MIN_BOOKING_LEAD_MINUTES)));
            if (meetsLeadTime && !overlapsLunch(probe) && !hasCapacityConflict(probe)) {
                slots.add(new AvailableSlotDTO(probe.getStartTime(), probe.getEndTime()));
            }
        }
        return slots;
    }

    /** Kiểm tra lịch hẹn hợp lệ trước khi lưu: thời gian, giờ làm, giờ nghỉ và sức chứa. */
    @Override
    public List<AvailableSlotDTO> findAvailableInstallationSlots(LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày đặt lịch không được nằm trong quá khứ.");
        }
        return INSTALLATION_SLOT_STARTS.stream()
                .map(start -> new AvailableSlotDTO(start, start.plusMinutes(INSTALLATION_DURATION_MINUTES)))
                .filter(slot -> {
                    Booking probe = new Booking();
                    probe.setBookingDate(date);
                    probe.setStartTime(slot.getStartTime());
                    probe.setEndTime(slot.getEndTime());
                    return !hasCapacityConflict(probe);
                })
                .toList();
    }

    @Override
    public void validateSlot(Booking booking) {
        if (booking.getBookingDate() == null || booking.getStartTime() == null || booking.getEndTime() == null) {
            throw new RuntimeException("Vui lòng chọn đầy đủ ngày và giờ hẹn.");
        }
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Bạn không thể đặt lịch trong quá khứ.");
        }
        LocalTime workStart = settingService.getTime(SystemSettingServiceImpl.WORK_START_TIME);
        LocalTime workEnd = settingService.getTime(SystemSettingServiceImpl.WORK_END_TIME);
        if (booking.getStartTime().isBefore(workStart) || booking.getEndTime().isAfter(workEnd)) {
            throw new RuntimeException("Khung giờ này nằm ngoài giờ làm việc của xưởng.");
        }
        LocalDateTime appointment = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        int minimumLeadMinutes = settingService.getInt(SystemSettingServiceImpl.MIN_BOOKING_LEAD_MINUTES);
        if (appointment.isBefore(LocalDateTime.now().plusMinutes(minimumLeadMinutes))) {
            throw new RuntimeException("Vui lòng đặt lịch trước giờ hẹn ít nhất " + minimumLeadMinutes + " phút.");
        }
        if (overlapsLunch(booking)) {
            throw new RuntimeException("Khung giờ đã chọn trùng với giờ nghỉ trưa của xưởng.");
        }
        if (hasCapacityConflict(booking)) {
            throw new RuntimeException("Khung giờ này đã kín. Vui lòng chọn giờ khác.");
        }
    }

    /** Giữ chỗ lịch hẹn bằng cách xác thực ngày xưởng hoạt động và khung giờ còn trống. */
    @Override
    public void holdSlot(Booking booking) {
        if (closedDateRepository.existsByClosedDate(booking.getBookingDate())) {
            throw new RuntimeException("Xưởng nghỉ vào ngày đã chọn. Vui lòng chọn ngày khác.");
        }
        validateSlot(booking);
    }

    /** Giải phóng chỗ lịch hẹn bằng cách chuyển lịch sang trạng thái đã hủy. */
    @Override
    public void releaseSlot(Booking booking) {
        booking.setBookingStatus(BookingStatus.CANCELED);
    }

    /** Tính tổng thời lượng cần dùng của các dịch vụ hợp lệ được chọn. */
    private int resolveDuration(List<Integer> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một dịch vụ.");
        }
        List<Integer> distinctIds = serviceIds.stream().distinct().toList();
        List<com.hsf302.carshowroom.entity.Service> services = serviceRepository.findAllById(distinctIds);
        if (services.size() != distinctIds.size()) {
            throw new IllegalArgumentException("Dịch vụ đã chọn không tồn tại.");
        }
        if (services.stream().anyMatch(service -> service.getStatus() != ServiceStatus.ACTIVE)) {
            throw new IllegalArgumentException("Dịch vụ đã chọn hiện đã ngừng hoạt động.");
        }
        return services.stream()
                .mapToInt(service -> service.getDurationMinutes() == null ? 60 : service.getDurationMinutes())
                .sum();
    }

    /** Kiểm tra số lịch đang chồng lấn có đạt sức chứa tối đa của xưởng hay không. */
    private boolean hasCapacityConflict(Booking booking) {
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.CONFIRMED,
                BookingStatus.WAITING_FOR_VEHICLE,
                BookingStatus.RECEIVING_VEHICLE,
                BookingStatus.IN_PROGRESS,
                BookingStatus.PENDING_APPROVAL
        );
        long overlaps = bookingRepository.findByBookingDateAndBookingStatusIn(booking.getBookingDate(), blockingStatuses)
                .stream()
                .filter(existing -> booking.getId() == null || !booking.getId().equals(existing.getId()))
                .filter(existing -> existing.getStartTime() != null && existing.getEndTime() != null)
                .filter(existing -> existing.getStartTime().isBefore(booking.getEndTime())
                        && existing.getEndTime().isAfter(booking.getStartTime()))
                .count();
        return overlaps >= settingService.getInt(SystemSettingServiceImpl.WORKSHOP_CAPACITY);
    }

    /** Kiểm tra khoảng thời gian lịch hẹn có giao với giờ nghỉ trưa được cấu hình. */
    private boolean overlapsLunch(Booking booking) {
        LocalTime lunchStart = settingService.getTime(SystemSettingServiceImpl.LUNCH_START_TIME);
        LocalTime lunchEnd = settingService.getTime(SystemSettingServiceImpl.LUNCH_END_TIME);
        return booking.getStartTime().isBefore(lunchEnd) && booking.getEndTime().isAfter(lunchStart);
    }
}
