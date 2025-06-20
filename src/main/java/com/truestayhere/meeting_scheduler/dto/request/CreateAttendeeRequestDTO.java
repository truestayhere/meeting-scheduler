package com.truestayhere.meeting_scheduler.dto.request;

import com.truestayhere.meeting_scheduler.dto.validation.ValidWorkingHours;
import com.truestayhere.meeting_scheduler.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@ValidWorkingHours
public record CreateAttendeeRequestDTO(

        @NotBlank(message = "Attendee name cannot be blank.")
        // Not null AND contains at least one non-whitespace character
        @Size(max = 100, message = "Attendee name cannot exceed 100 characters.")
        String name,

        @NotBlank(message = "Attendee email cannot be blank.")
        @Email(message = "Invalid email format.")
        @Size(max = 100, message = "Email cannot exceed 100 characters.")
        String email,


        @NotBlank(message = "Password cannot be blank.")
        @Size(min = 8, message = "Password must be at leat 8 characters long.")
        String password,

        Role role,

        LocalTime workingStartTime,
        LocalTime workingEndTime
) {
}
