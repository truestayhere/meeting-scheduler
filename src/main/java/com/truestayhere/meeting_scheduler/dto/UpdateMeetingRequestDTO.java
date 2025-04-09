package com.truestayhere.meeting_scheduler.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UpdateMeetingRequestDTO(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long locationId,
        Set<Long> attendeeIds
) {
}
