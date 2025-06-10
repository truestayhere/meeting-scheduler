package com.truestayhere.meeting_scheduler.dto.request;

import com.truestayhere.meeting_scheduler.dto.validation.NullButNotBlank;
import com.truestayhere.meeting_scheduler.dto.validation.NullButNotEmpty;
import com.truestayhere.meeting_scheduler.dto.validation.StartBeforeEnd;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

@StartBeforeEnd
@NullButNotEmpty
@NullButNotBlank
public record UpdateMeetingRequestDTO(
        @Size(max = 200, message = "Meeting title must not exceed 200 characters.")
        String title,

        @FutureOrPresent(message = "Meeting start time cannot be set in the past.")
        LocalDateTime startTime,

        LocalDateTime endTime,

        Long locationId,

        Set<@NotNull Long> attendeeIds
) {
}
