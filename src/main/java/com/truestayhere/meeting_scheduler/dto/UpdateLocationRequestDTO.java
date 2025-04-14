package com.truestayhere.meeting_scheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLocationRequestDTO(
        @NotBlank(message = "Location name cannot be blank.")
        @Size(max = 150, message = "Location name cannot exceed 150 characters.")
        String name,

        @Min(value = 1, message = "Capacity must be at least 1 if provided.")
        Integer capacity
) {
}
