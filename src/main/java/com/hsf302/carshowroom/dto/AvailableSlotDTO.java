package com.hsf302.carshowroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDTO {
    private LocalTime startTime;
    private LocalTime endTime;
}
