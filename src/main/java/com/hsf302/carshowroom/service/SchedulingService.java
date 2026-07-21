package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.AvailableSlotDTO;
import com.hsf302.carshowroom.entity.Booking;

import java.time.LocalDate;
import java.util.List;

public interface SchedulingService {
    List<AvailableSlotDTO> findAvailableSlots(LocalDate date, List<Integer> serviceIds);
    void validateSlot(Booking booking);
    void holdSlot(Booking booking);
    void releaseSlot(Booking booking);
}
