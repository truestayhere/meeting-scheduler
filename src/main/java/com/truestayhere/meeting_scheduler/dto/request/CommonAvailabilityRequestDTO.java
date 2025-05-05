package com.truestayhere.meeting_scheduler.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

public record CommonAvailabilityRequestDTO(
        @NotEmpty(message = "At least one attendee ID must be provided.")
        Set<@NotNull Long> attendeeIds,

        @NotNull(message = "A date must be provided.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
) {
}
