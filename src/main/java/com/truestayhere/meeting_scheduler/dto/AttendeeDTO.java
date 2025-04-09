package com.truestayhere.meeting_scheduler.dto;

// Java records are immutable by default.
// Constructor with all fields, getters, toString, equals and HashCode methods are generated automatically.
public record AttendeeDTO (
        Long id,
        String name,
        String email
) {

}
