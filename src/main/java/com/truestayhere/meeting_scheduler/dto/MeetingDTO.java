package com.truestayhere.meeting_scheduler.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record MeetingDTO(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocationDTO location, // Embedding the LocationDTO
        Set<AttendeeDTO> attendees // Embedding the AttendeeDTO
) {
}
