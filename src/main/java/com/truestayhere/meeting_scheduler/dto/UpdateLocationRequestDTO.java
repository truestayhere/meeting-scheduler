package com.truestayhere.meeting_scheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UpdateLocationRequestDTO(

        @NotBlank(message = "Location name cannot be blank.")
        @Size(max = 150, message = "Location name cannot exceed 150 characters.")
        String name,

        @Min(value = 1, message = "Capacity must be at least 1 if provided.")
        Integer capacity,

        @NotBlank(message = "Location working start time cannot be blank.")
        LocalTime workingStartTime,

        @NotBlank(message = "Location working end time cannot be blank.")
        LocalTime workingEndTime
) {
}
