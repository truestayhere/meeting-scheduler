package com.truestayhere.meeting_scheduler.exception;


import com.truestayhere.meeting_scheduler.dto.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice // Marks this class as a global exception handler
public class GlobalExceptionHandler {

    // "static" ensures that there is only one Logger instance per class
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handler for @Valid Bean Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation failed for request {}: {}", request.getRequestURI(), ex.getMessage());

        // It's ok to use forEach() here as well, but stream() improves readability
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " +
                        fieldError.getDefaultMessage())
                .collect(Collectors.toList());

        // Add global errors to the errors list
        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            errors.add(globalError.getObjectName() + ": " + globalError.getDefaultMessage());
        });

        // Map to ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400 BAD REQUEST
    }

    // Handler for data not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found for request {}: {}", request.getRequestURI(), ex.getMessage());

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND); // 404 NOT FOUND
    }

    // Handler for business logic errors (duplicates, overlap etc)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid argument/state for request {}: {}", request.getRequestURI(), ex.getMessage());

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument/State",
                ex.getMessage(),
                request.getRequestURI()
        );
        // can return 400 BAD REQUEST or 409 CONFLICT depending on context
        // use 400 (for now)
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400 BAD REQUEST
    }

    // Handler for database integrity constraint violations (unique, not null etc)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Database constraint violation occurred.";

        // Provide specific message if possible
        if (ex.getMessage() != null && ex.getMessage().contains("ConstraintViolationException")) {
            message = "Data integrity violation: Check unique constraints or foreign keys.";
        }

        log.warn("DataIntegrityViolationException: {} on path {}", message, request.getRequestURI(), ex);

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Data Conflict/Integrity Violation",
                message,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // 409 CONFLICT
    }

    // Handler for invalid JSON request body or incorrect data type/format
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("HttpMessageNotReadableException: {} on path {}", ex.getMessage(), request.getRequestURI());

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed Request Body",
                "Request body is malformed or contains invalid data/format.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400 BAD REQUEST
    }


    // Handler for unsupported method calls
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("HttpRequestMethodNotSupportedException: Method '{}' not supported for path {}", ex.getMethod(), request.getRequestURI());

        // Build string containing supported methods
        StringBuilder supportedMethods = new StringBuilder();
        if (ex.getSupportedHttpMethods() != null) {
            ex.getSupportedHttpMethods().forEach(method -> supportedMethods.append(method).append(" "));
        }

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                "HTTP method " + ex.getMethod() + " is not supported for this request." +
                        "Supported methods are: " + supportedMethods.toString().trim(),
                request.getRequestURI()
        );

        // Must add "Allow" header for status 405 according to HTTP specification
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED) // 405 METHOD NOT ALLOWED
                .header("Allow", supportedMethods.toString().trim())
                .body(errorResponse);

    }


    // Handler for scheduling conflicts/overlaps
    @ExceptionHandler(MeetingConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleMeetingConflictException(
            MeetingConflictException ex, HttpServletRequest request) {

        log.warn("MeetingConflictException: {} on path {}", ex.getMessage(), request.getRequestURI());

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Scheduling conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // 409 CONFLICT
    }


    // Handler for any other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception occurred processing request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        // passing the exception "ex" as a last argument includes the stack trace in the log output

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500 INTERNAL SERVER ERROR
    }

}
