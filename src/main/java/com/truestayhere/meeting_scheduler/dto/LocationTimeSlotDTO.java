package com.truestayhere.meeting_scheduler.dto;

public record LocationTimeSlotDTO(
        LocationDTO location,
        AvailableSlotDTO availableSlot
) {
}
