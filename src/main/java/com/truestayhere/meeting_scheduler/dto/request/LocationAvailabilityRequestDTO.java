package com.truestayhere.meeting_scheduler.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record LocationAvailabilityRequestDTO(
        @NotNull(message = "A date must be provided.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        @NotNull(message = "Minimum duration must be provided.")
        @Min(value = 1, message = "Duration must be at least 1 minute.")
        Integer durationMinutes,

        @Min(value = 1, message = "Minimum capacity must be at least 1 if provided.")
        Integer minimumCapacity
) {
}
