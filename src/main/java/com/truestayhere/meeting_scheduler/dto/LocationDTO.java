package com.truestayhere.meeting_scheduler.dto;

public record LocationDTO(
        Long id,
        String name,
        Integer capacity // Type Integer allows the possibility of null values
) {
}
