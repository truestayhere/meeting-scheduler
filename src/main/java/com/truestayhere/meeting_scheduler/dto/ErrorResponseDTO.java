package com.truestayhere.meeting_scheduler.dto;


import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(
        LocalDateTime timestamp,
        Integer status,
        String error,
        List<String> messages,
        String path // The URL path where the error occurred
) {
    // Constructor for single message errors
    public ErrorResponseDTO(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, List.of(message), path);
    }
}
