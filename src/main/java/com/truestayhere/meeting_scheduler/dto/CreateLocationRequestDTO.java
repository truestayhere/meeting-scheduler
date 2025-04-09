package com.truestayhere.meeting_scheduler.dto;

public record CreateLocationRequestDTO(
        String name,
        Integer capacity
) {
}
