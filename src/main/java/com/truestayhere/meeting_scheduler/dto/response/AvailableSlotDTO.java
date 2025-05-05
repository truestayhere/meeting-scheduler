package com.truestayhere.meeting_scheduler.dto.response;

import java.time.LocalDateTime;

public record AvailableSlotDTO(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
