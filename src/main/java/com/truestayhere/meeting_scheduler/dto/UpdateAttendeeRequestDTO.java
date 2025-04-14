package com.truestayhere.meeting_scheduler.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAttendeeRequestDTO(

        @NotBlank(message = "Attendee name cannot be blank.")
        @Size(max = 100, message = "Attendee name cannot exceed 100 characters.")
        String name,

        @NotBlank(message = "Attendee email cannot be blank.")
        @Email(message = "Invalid email format.")
        @Size(max = 100, message = "Email cannot exceed 100 characters.")
        String email
) {
}
