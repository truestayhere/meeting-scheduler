package com.truestayhere.meeting_scheduler.dto;

import java.time.LocalDateTime;

public record MeetingSuggestionDTO(
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocationDTO locationDTO
) {
}
