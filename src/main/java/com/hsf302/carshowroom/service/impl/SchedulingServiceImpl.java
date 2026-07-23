package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.dto.AvailableSlotDTO;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService {
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final int SLOT_STEP_MINUTES = 30;
    private static final int WORKSHOP_CAPACITY = 2;
    private static final int INSTALLATION_DURATION_MINUTES = 120;
    private static final List<LocalTime> INSTALLATION_SLOT_STARTS = List.of(
            LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(13, 0), LocalTime.of(15, 0));

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    @Override
    public List<AvailableSlotDTO> findAvailableSlots(LocalDate date, List<Integer> serviceIds) {
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày đặt lịch không được nằm trong quá khứ.");
        }
        int duration = resolveDuration(serviceIds);
        List<AvailableSlotDTO> slots = new ArrayList<>();
        for (LocalTime start = WORK_START; !start.plusMinutes(duration).isAfter(WORK_END); start = start.plusMinutes(SLOT_STEP_MINUTES)) {
            Booking probe = new Booking();
            probe.setBookingDate(date);
            probe.setStartTime(start);
            probe.setEndTime(start.plusMinutes(duration));
            if (!hasCapacityConflict(probe)) {
                slots.add(new AvailableSlotDTO(probe.getStartTime(), probe.getEndTime()));
            }
        }
        return slots;
    }

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
        if (booking.getStartTime().isBefore(WORK_START) || booking.getEndTime().isAfter(WORK_END)) {
            throw new RuntimeException("Khung giờ này nằm ngoài giờ làm việc của xưởng.");
        }
        if (hasCapacityConflict(booking)) {
            throw new RuntimeException("Khung giờ này đã kín. Vui lòng chọn giờ khác.");
        }
    }

    @Override
    public void holdSlot(Booking booking) {
        validateSlot(booking);
    }

    @Override
    public void releaseSlot(Booking booking) {
        booking.setBookingStatus(BookingStatus.CANCELED);
    }

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
        return overlaps >= WORKSHOP_CAPACITY;
    }
}
