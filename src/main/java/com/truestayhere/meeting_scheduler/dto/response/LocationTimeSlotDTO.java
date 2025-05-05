package com.truestayhere.meeting_scheduler.dto.response;

public record LocationTimeSlotDTO(
        LocationDTO location,
        AvailableSlotDTO availableSlot
) {
}
