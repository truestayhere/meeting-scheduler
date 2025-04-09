package com.truestayhere.meeting_scheduler.dto;

// Data validation will be added later!

import java.time.LocalDateTime;
import java.util.Set;

// Request send from client to create a new meeting
public record CreateMeetingRequestDTO(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long locationId, // We only need to know a location id, not all location data to create a meeting
        Set<Long> attendeeIds // No need to send all attendee data, only id's to create a meeting
) {
}
