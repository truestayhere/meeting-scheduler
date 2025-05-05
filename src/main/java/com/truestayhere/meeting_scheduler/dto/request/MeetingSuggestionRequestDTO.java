package com.truestayhere.meeting_scheduler.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

public record MeetingSuggestionRequestDTO(
        @NotEmpty(message = "Attendee list cannot be empty.")
        Set<@NotNull Long> attendeeIds,

        @NotNull(message = "Meeting duration cannot be empty.")
        @Min(value = 1, message = "Duration must me at least 1 minute.")
        Integer durationMinutes,

        @NotNull(message = "A date must be provided.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
) {
}
