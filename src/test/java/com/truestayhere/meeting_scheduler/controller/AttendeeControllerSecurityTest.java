package com.truestayhere.meeting_scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.config.CustomAuthenticationEntryPoint;
import com.truestayhere.meeting_scheduler.config.SecurityConfig;
import com.truestayhere.meeting_scheduler.dto.request.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.exception.GlobalExceptionHandler;
import com.truestayhere.meeting_scheduler.helper.AttendeeTestHelper;
import com.truestayhere.meeting_scheduler.model.Role;
import com.truestayhere.meeting_scheduler.service.AttendeeService;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendeeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class AttendeeControllerSecurityTest {

    @MockitoBean
    AttendeeService attendeeService;
    @MockitoBean
    AvailabilityService availabilityService;
    @MockitoBean
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private AttendeeTestHelper attendeeTestHelper;
    private CreateAttendeeRequestDTO newAttendee;
    private UpdateAttendeeRequestDTO updateAttendee;
    private Long attendeeId;

    @BeforeEach
    void setUp() {
        attendeeTestHelper = new AttendeeTestHelper(mockMvc, objectMapper);
        attendeeId = 1L;

        newAttendee = new CreateAttendeeRequestDTO(
                "Test Attendee",
                "attendee@test.com",
                "rawPassword",
                Role.USER,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        updateAttendee = new UpdateAttendeeRequestDTO(
                "Updated Attendee",
                null,
                null,
                null,
                null,
                null
        );
    }

    static Stream<Arguments> attendeeOperationsProvider() {
        return Stream.of(
                Arguments.of("Create", HttpStatus.CREATED),
                Arguments.of("Update", HttpStatus.OK),
                Arguments.of("Delete", HttpStatus.NO_CONTENT)
        );
    }

    @ParameterizedTest(name = "{0}: should return {1}")
    @MethodSource("attendeeOperationsProvider")
    @WithMockUser(authorities = {"ADMIN"})
    void whenAdminPerformsAttendeeOperations_thenReturnsSuccess(
            String operationName,
            HttpStatus expectedStatus) throws Exception {

        ResultActions resultActions = executeOperation(operationName);
        resultActions.andExpect(status().is(expectedStatus.value()));
    }

    @ParameterizedTest(name = "{0}: should return FORBIDDEN")
    @MethodSource("attendeeOperationsProvider")
    @WithMockUser(authorities = {"USER"})
    void whenUserPerformsAttendeeOperations_thenReturnsForbidden(
            String operationName,
            HttpStatus expectedSuccessStatus) throws Exception {

        ResultActions resultActions = executeOperation(operationName);
        resultActions.andExpect(status().isForbidden());
    }

    private ResultActions executeOperation(String operation) throws Exception {
        return switch (operation) {
            case "Create" -> attendeeTestHelper.performCreateAttendee(newAttendee);
            case "Update" -> attendeeTestHelper.performUpdateAttendee(attendeeId, updateAttendee);
            case "Delete" -> attendeeTestHelper.performDeleteAttendee(attendeeId);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

}
