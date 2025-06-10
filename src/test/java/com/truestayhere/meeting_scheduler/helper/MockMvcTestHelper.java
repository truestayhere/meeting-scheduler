package com.truestayhere.meeting_scheduler.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Generic helper class for MockMvc controller integration tests.
 * Provides reusable methods for common HTTP operations and assertions.
 */
@RequiredArgsConstructor
public class MockMvcTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    // === REQUEST BUILDERS ===

    /**
     * Performs POST request to create a resource
     */
    public ResultActions performCreate(String endpoint, Object request) throws Exception {
        return mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    /**
     * Performs PUT request to update a resource
     */
    public ResultActions performUpdate(String endpoint, Long id, Object request) throws Exception {
        return mockMvc.perform(put(endpoint + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    /**
     * Performs GET request to retrieve all resources
     */
    public ResultActions performGetAll(String endpoint) throws Exception {
        return mockMvc.perform(get(endpoint)
                .accept(MediaType.APPLICATION_JSON));
    }

    /**
     * Performs GET request to retrieve a resource by ID
     */
    public ResultActions performGetById(String endpoint, Long id) throws Exception {
        return mockMvc.perform(get(endpoint + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON));
    }

    /**
     * Performs DELETE request to delete a resource by ID
     */
    public ResultActions performDeleteById(String endpoint, Long id) throws Exception {
        return mockMvc.perform(delete(endpoint + "/{id}", id));
    }

    /**
     * Performs GET request for availability with date parameter
     */
    public ResultActions performGetAvailability(String endpoint, Long id, LocalDate date) throws Exception {
        return mockMvc.perform(get(endpoint + "/{id}/availability", id)
                .param("date", date.toString())
                .accept(MediaType.APPLICATION_JSON));
    }

    /**
     * Performs GET request with custom endpoint and range parameters
     */
    public ResultActions performGetMeetingsBy(String endpoint, Long id, LocalDateTime rangeStart, LocalDateTime rangeEnd) throws Exception {
        return mockMvc.perform(get(endpoint + "/{id}", id)
                .param("start", rangeStart.toString())
                .param("end", rangeEnd.toString())
                .accept(MediaType.APPLICATION_JSON));
    }

    /**
     * Performs POST request with custom endpoint and request body
     */
    public ResultActions performPostRequest(String endpoint, Object request) throws Exception {
        return mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON));
    }

    // === RESPONSE ASSERTIONS ===

    /**
     * Asserts successful response (200) with JSON content type
     */
    public void assertSuccessResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Asserts created response (201) with JSON content type
     */
    public void assertCreatedResponse(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Asserts successful response
     */
    public <T> void assertEntityResponse(ResultActions resultActions, T expected,
                                         EntityValidator<T> validator) throws Exception {
        assertSuccessResponse(resultActions);
        validator.validate(resultActions, expected);
    }

    /**
     * Asserts created response
     */
    public <T> void assertCreatedEntityResponse(ResultActions resultActions, T expected,
                                                EntityValidator<T> validator) throws Exception {
        assertCreatedResponse(resultActions);
        validator.validate(resultActions, expected);
    }

    /**
     * Asserts successful list response
     */
    public <T> void assertEntityListResponse(ResultActions resultActions, List<T> expected,
                                             EntityValidator<List<T>> validator) throws Exception {
        assertSuccessResponse(resultActions);
        resultActions.andExpect(jsonPath("$.length()", is(expected.size())));
        validator.validate(resultActions, expected);
    }

    /**
     * Generic error response assertion
     */
    public void assertErrorResponse(ResultActions resultActions, HttpStatus expectedStatus,
                                    String expectedError, String expectedMessage) throws Exception {
        resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(expectedStatus.value())))
                .andExpect(jsonPath("$.error", is(expectedError)))
                .andExpect(jsonPath("$.messages[0]", is(expectedMessage)));
    }

    // === ERROR RESPONSE ASSERTIONS ===

    /**
     * Asserts validation error (400)
     */
    public void assertValidationError(ResultActions resultActions, String expectedTarget,
                                      String expectedMessage) throws Exception {
        assertErrorResponse(resultActions, HttpStatus.BAD_REQUEST, "Validation Failed",
                expectedTarget + ": " + expectedMessage);
    }

    /**
     * Asserts not found error (404)
     */
    public void assertNotFoundError(ResultActions resultActions, String expectedMessage) throws Exception {
        assertErrorResponse(resultActions, HttpStatus.NOT_FOUND, "Resource Not Found", expectedMessage);
    }

    /**
     * Asserts conflict error (409)
     */
    public void assertConflictError(ResultActions resultActions, String expectedMessage) throws Exception {
        assertErrorResponse(resultActions, HttpStatus.CONFLICT, "Data Conflict/Integrity Violation", expectedMessage);
    }

    /**
     * Asserts parameter type error
     */
    public void assertParameterTypeError(ResultActions resultActions, String expectedMessage) throws Exception {
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is("Invalid Parameter Type/Format")))
                .andExpect(jsonPath("$.messages[0]", containsString(expectedMessage)));
    }

    /**
     * Asserts malformed request body error
     */
    public void assertMalformedRequestError(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", containsString("Malformed Request Body")))
                .andExpect(jsonPath("$.messages[0]", containsString("Request body is malformed or contains invalid data/format.")));
    }

    /**
     * Functional interface for entity validation
     */
    @FunctionalInterface
    public interface EntityValidator<T> {
        void validate(ResultActions resultActions, T expected) throws Exception;
    }
}