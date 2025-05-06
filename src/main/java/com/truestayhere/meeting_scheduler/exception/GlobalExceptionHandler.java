package com.truestayhere.meeting_scheduler.exception;


import com.truestayhere.meeting_scheduler.dto.response.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice // Marks this class as a global exception handler
@Slf4j
public class GlobalExceptionHandler {

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

    // Handler for type mismatch exceptions
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String parameterName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        Object providedValue = ex.getValue();

        String errorMessage = String.format("Parameter '%s' should be of type '%s' but received value: '%s'.",
                parameterName,
                requiredType,
                providedValue);

        log.warn("MethodArgumentTypeMismatchException: {} on path {}", errorMessage, request.getRequestURI());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Parameter Type/Format",
                errorMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400 BAD REQUEST
    }


    // Handle missing request parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String parameterName = ex.getParameterName();
        String parameterType = ex.getParameterType();
        String errorMessage = String.format("Required parameter '%s' of type '%s' is missing.", parameterName, parameterType);

        log.warn("MissingServletRequestParameterException: {} on path {}", errorMessage, request.getRequestURI());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Missing Request Parameter",
                errorMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400 BAD REQUEST
    }

    // Handle authentication fail
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failure for request [{}]: {}", request.getRequestURI(), ex.getMessage());

        String message = "Authentication failed: Invalid credentials or user not found.";

        // Map to single-message ErrorResponseDTO
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED); // 401 UNAUTHORIZED
    }


    // Handle deletion of resource already in use exception
    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceInUseException(
            ResourceInUseException ex, HttpServletRequest request) {

        // Construct the message list
        List<String> messages = new ArrayList<>();
        messages.add(ex.getMessage());

        // Add the conflicting meeting Ids to the list
        if (ex.getConflictingMeetingIds() != null && !ex.getConflictingMeetingIds().isEmpty()) {
            messages.add("Conflicting Meeting Ids: " +
                    ex.getConflictingMeetingIds().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")));
        }

        // Map to ErrorResponse DTO with the message list
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                messages,
                request.getRequestURI()
        );

        log.warn("Resource conflict on request [{}]: Message: {}, Conflicting IDs: {}", request.getRequestURI(), ex.getMessage(), ex.getConflictingMeetingIds());
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
