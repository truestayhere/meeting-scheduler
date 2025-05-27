package com.truestayhere.meeting_scheduler.dto.request;

import com.truestayhere.meeting_scheduler.dto.validation.ValidWorkingHours;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@ValidWorkingHours
public record UpdateLocationRequestDTO(

        @Size(max = 150, message = "Location name cannot exceed 150 characters.")
        String name,

        @Min(value = 1, message = "Location capacity must be at least 1.")
        Integer capacity,

        LocalTime workingStartTime,
        LocalTime workingEndTime
) {
}
