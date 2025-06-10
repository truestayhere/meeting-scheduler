package com.truestayhere.meeting_scheduler.dto.request;

import com.truestayhere.meeting_scheduler.dto.validation.ValidWorkingHours;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@ValidWorkingHours
public record UpdateAttendeeRequestDTO(

        @Size(max = 100, message = "Attendee name cannot exceed 100 characters.")
        String name,

        @Email(message = "Invalid email format.")
        @Size(max = 100, message = "Email cannot exceed 100 characters.")
        String email,

        @Size(min = 8, message = "Password must be at leat 8 characters long.")
        String password,

        String role,

        LocalTime workingStartTime,
        LocalTime workingEndTime
) {
}
