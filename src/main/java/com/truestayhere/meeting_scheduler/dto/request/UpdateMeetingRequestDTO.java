package com.truestayhere.meeting_scheduler.dto.request;

import com.truestayhere.meeting_scheduler.dto.validation.StartBeforeEnd;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Set;

@StartBeforeEnd
public record UpdateMeetingRequestDTO(
        @NotBlank(message = "Meeting title must not be blank.")
        @Size(max = 200, message = "Meeting title must not exceed 200 characters.")
        String title,

        @NotNull(message = "Meeting start time cannot be null.")
        @FutureOrPresent(message = "Meeting start time cannot be set in the past.")
        LocalDateTime startTime,

        @NotNull(message = "Meeting end time cannot be null.")
        LocalDateTime endTime,

        @NotNull(message = "Meeting location ID cannot be null.")
        Long locationId,

        @NotEmpty(message = "Attendee list cannot be empty.") // @NotEmpty used for Collections
        Set<@NotNull Long> attendeeIds
) {
}
