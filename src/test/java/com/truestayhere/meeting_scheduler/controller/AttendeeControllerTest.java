package com.truestayhere.meeting_scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.config.CustomAuthenticationEntryPoint;
import com.truestayhere.meeting_scheduler.config.SecurityConfig;
import com.truestayhere.meeting_scheduler.dto.request.CommonAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.response.AvailableSlotDTO;
import com.truestayhere.meeting_scheduler.exception.GlobalExceptionHandler;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.helper.AttendeeTestHelper;
import com.truestayhere.meeting_scheduler.model.Role;
import com.truestayhere.meeting_scheduler.service.AttendeeService;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AttendeeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class AttendeeControllerTest {

    private final LocalDate DEFAULT_DATE = LocalDate.of(Year.now().getValue() + 1, 8, 14);
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
    private CreateAttendeeRequestDTO createRequest;
    private UpdateAttendeeRequestDTO updateRequest;
    private AttendeeDTO attendeeDTO1, attendeeDTO2;

    @BeforeEach
    void setUp() {
        attendeeTestHelper = new AttendeeTestHelper(mockMvc, objectMapper);

        attendeeDTO1 = new AttendeeDTO(
                1L,
                "Attendee One",
                "attendeeone@test.com"
        );

        attendeeDTO2 = new AttendeeDTO(
                2L,
                "Attendee Two",
                "attendeetwo@test.com"
        );

        createRequest = new CreateAttendeeRequestDTO(
                attendeeDTO1.name(),
                attendeeDTO1.email(),
                "rawPassword",
                Role.USER,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        updateRequest = new UpdateAttendeeRequestDTO(
                attendeeDTO1.name() + " Updated",
                "attendeeupdated@test.com",
                "rawPasswordUpdated",
                Role.ADMIN,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );
    }

    private static Stream<Arguments> invalidCreateRequestProvider() {

        String validName = "Attendee Name";
        String longName = "a".repeat(101);
        String validEmail = "valid@test.com";
        String longEmail = "a".repeat(64) + "@" + "b".repeat(36) + ".com";
        String invalidEmail = "invalidEmailFormat";
        String validPassword = "validRawPassword";
        String shortPassword = "a".repeat(7);
        Role validRole = Role.USER;
        LocalTime validStartTime = LocalTime.of(9, 0);
        LocalTime validEndTime = LocalTime.of(17, 0);
        LocalTime sameTime = LocalTime.of(10, 0);

        return Stream.of(
                Arguments.of(
                        "Name is null/blank",
                        new CreateAttendeeRequestDTO(null, validEmail, validPassword, validRole, validStartTime, validEndTime),
                        "name",
                        "Attendee name cannot be blank."
                ),
                Arguments.of(
                        "Name longer than max",
                        new CreateAttendeeRequestDTO(longName, validEmail, validPassword, validRole, validStartTime, validEndTime),
                        "name",
                        "Attendee name cannot exceed 100 characters."
                ),
                Arguments.of(
                        "Email is null/blank",
                        new CreateAttendeeRequestDTO(validName, null, validPassword, validRole, validStartTime, validEndTime),
                        "email",
                        "Attendee email cannot be blank."
                ),
                Arguments.of(
                        "Email longer that max",
                        new CreateAttendeeRequestDTO(validName, longEmail, validPassword, validRole, validStartTime, validEndTime),
                        "email",
                        "Email cannot exceed 100 characters."
                ),
                Arguments.of(
                        "Email is invalid format",
                        new CreateAttendeeRequestDTO(validName, invalidEmail, validPassword, validRole, validStartTime, validEndTime),
                        "email",
                        "Invalid email format."
                ),
                Arguments.of(
                        "Password is null/blank",
                        new CreateAttendeeRequestDTO(validName, validEmail, null, validRole, validStartTime, validEndTime),
                        "password",
                        "Password cannot be blank."
                ),
                Arguments.of(
                        "Password is less than min",
                        new CreateAttendeeRequestDTO(validName, validEmail, shortPassword, validRole, validStartTime, validEndTime),
                        "password",
                        "Password must be at leat 8 characters long."
                ),
                Arguments.of(
                        "Working start time equals end time",
                        new CreateAttendeeRequestDTO(validName, validEmail, validPassword, validRole, sameTime, sameTime),
                        "createAttendeeRequestDTO",
                        "Working start time and end time cannot be the same."
                )
        );
    }

    private static Stream<Arguments> invalidUpdateRequestProvider() {

        String validName = "Attendee Name Updated";
        String longName = "a".repeat(101);
        String validEmail = "validupdated@test.com";
        String longEmail = "a".repeat(64) + "@" + "b".repeat(36) + ".com";
        String invalidEmail = "invalidEmailFormat";
        String validPassword = "validUpdatedRawPassword";
        String shortPassword = "a".repeat(7);
        Role validRole = Role.USER;
        LocalTime validStartTime = LocalTime.of(10, 0);
        LocalTime validEndTime = LocalTime.of(18, 0);
        LocalTime sameTime = LocalTime.of(10, 0);

        return Stream.of(
                Arguments.of(
                        "Name longer than max",
                        new UpdateAttendeeRequestDTO(longName, validEmail, validPassword, validRole, validStartTime, validEndTime),
                        "name",
                        "Attendee name cannot exceed 100 characters."
                ),
                Arguments.of(
                        "Email longer that max",
                        new UpdateAttendeeRequestDTO(validName, longEmail, validPassword, validRole, validStartTime, validEndTime),
                        "email",
                        "Email cannot exceed 100 characters."
                ),
                Arguments.of(
                        "Email is invalid format",
                        new UpdateAttendeeRequestDTO(validName, invalidEmail, validPassword, validRole, validStartTime, validEndTime),
                        "email",
                        "Invalid email format."
                ),
                Arguments.of(
                        "Password is less than min",
                        new UpdateAttendeeRequestDTO(validName, validEmail, shortPassword, validRole, validStartTime, validEndTime),
                        "password",
                        "Password must be at leat 8 characters long."
                ),
                Arguments.of(
                        "Working start time equals end time",
                        new UpdateAttendeeRequestDTO(validName, validEmail, validPassword, validRole, sameTime, sameTime),
                        "updateAttendeeRequestDTO",
                        "Working start time and end time cannot be the same."
                )
        );
    }

    private static Stream<Arguments> invalidCommonAvailabilityRequestProvider() {

        Set<Long> validAttendeeIds = Set.of(1L, 2L);
        Set<Long> attendeeIdsWithNullElement = new HashSet<>();
        attendeeIdsWithNullElement.add(1L);
        attendeeIdsWithNullElement.add(null);
        LocalDate validDate = LocalDate.of(Year.now().getValue() + 1, 8, 14);

        return Stream.of(
                Arguments.of(
                        "AttendeeIds is null/empty",
                        new CommonAvailabilityRequestDTO(Set.of(), validDate),
                        "attendeeIds",
                        "At least one attendee ID must be provided."
                ),
                Arguments.of(
                        "AttendeeIds contains a null element",
                        new CommonAvailabilityRequestDTO(attendeeIdsWithNullElement, validDate),
                        "attendeeIds[]",
                        "must not be null"
                ),
                Arguments.of(
                        "Date is null",
                        new CommonAvailabilityRequestDTO(validAttendeeIds, null),
                        "date",
                        "A date must be provided."
                )
        );
    }

    // === CREATE ===

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void createAttendee_whenValidInput_shouldReturn201CreatedAndAttendeeResponse() throws Exception {
        when(attendeeService.createAttendee(any(CreateAttendeeRequestDTO.class))).thenReturn(attendeeDTO1);

        ResultActions resultActions = attendeeTestHelper.performCreateAttendee(createRequest);

        attendeeTestHelper.assertCreatedAttendeeResponse(resultActions, attendeeDTO1);

        verify(attendeeService).createAttendee(any(CreateAttendeeRequestDTO.class));
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidCreateRequestProvider")
    @WithMockUser(authorities = {"ADMIN"})
    void createAttendee_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            CreateAttendeeRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {

        ResultActions resultActions = attendeeTestHelper.performCreateAttendee(invalidRequest);

        attendeeTestHelper.assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(attendeeService, never()).createAttendee(any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void createAttendee_whenWorkingTimeIsMalformedString_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "name": "Attendee Name",
                    "email": "attendeename@test.com",
                    "password": "rawPassword",
                    "role": "USER",
                    "workingStartTime": "INVALID-TIME-FORMAT",
                    "workingEndTime": "17:00"
                }
                """;
        ResultActions resultActions = mockMvc.perform(post("/api/attendees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        attendeeTestHelper.assertMalformedRequestError(resultActions);

        verify(attendeeService, never()).createAttendee(any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void createAttendee_whenRoleIsInvalid_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "name": "Attendee Name",
                    "email": "attendeename@test.com",
                    "password": "rawPassword",
                    "role": "INVALID_ROLE",
                    "workingStartTime": "09:00",
                    "workingEndTime": "17:00"
                }
                """;
        ResultActions resultActions = mockMvc.perform(post("/api/attendees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        attendeeTestHelper.assertMalformedRequestError(resultActions);

        verify(attendeeService, never()).createAttendee(any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void createAttendee_whenServiceThrowsDataIntegrityException_shouldReturn409Conflict() throws Exception {
        String expectedErrorMessage = "Database constraint violation occurred.";
        when(attendeeService.createAttendee(any(CreateAttendeeRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException(expectedErrorMessage));

        ResultActions resultActions = attendeeTestHelper.performCreateAttendee(createRequest);

        attendeeTestHelper.assertConflictError(resultActions, expectedErrorMessage);

        verify(attendeeService).createAttendee(any(CreateAttendeeRequestDTO.class));
    }

    // === END CREATE ===

    // === GET ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAllAttendees_shouldReturn200OkAndListOfAttendees() throws Exception {
        List<AttendeeDTO> expectedAttendees = List.of(attendeeDTO1, attendeeDTO2);

        when(attendeeService.getAllAttendees()).thenReturn(expectedAttendees);

        ResultActions resultActions = attendeeTestHelper.performGetAllAttendees();

        attendeeTestHelper.assertAttendeeListResponse(resultActions, expectedAttendees);

        verify(attendeeService).getAllAttendees();
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAllAttendees_whenNoAttendees_shouldReturn200OkAndEmptyList() throws Exception {
        when(attendeeService.getAllAttendees()).thenReturn(List.of());

        ResultActions resultActions = attendeeTestHelper.performGetAllAttendees();

        attendeeTestHelper.assertAttendeeListResponse(resultActions, List.of());

        verify(attendeeService).getAllAttendees();
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeById_whenAttendeeExists_shouldReturn200OkAndAttendeeResponse() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        when(attendeeService.getAttendeeById(attendeeId)).thenReturn(attendeeDTO1);

        ResultActions resultActions = attendeeTestHelper.performGetAttendee(attendeeId);

        attendeeTestHelper.assertAttendeeResponse(resultActions, attendeeDTO1);

        verify(attendeeService).getAttendeeById(attendeeId);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeById_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(attendeeService.getAttendeeById(nonExistentAttendeeId)).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = attendeeTestHelper.performGetAttendee(nonExistentAttendeeId);

        attendeeTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(attendeeService).getAttendeeById(nonExistentAttendeeId);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(get("/api/attendees/{id}", invalidId)
                .accept(MediaType.APPLICATION_JSON));

        attendeeTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);
    }

    // === END GET ===

    // === UPDATE ===

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendeeById_whenValidInputAndAttendeeExists_shouldReturn200OkAndUpdatedAttendeeResponse() throws Exception {
        Long attendeeIdToUpdate = attendeeDTO1.id();

        AttendeeDTO updatedAttendeeDTO = new AttendeeDTO(
                attendeeIdToUpdate,
                updateRequest.name(),
                updateRequest.email()
        );

        when(attendeeService.updateAttendee(eq(attendeeIdToUpdate), any(UpdateAttendeeRequestDTO.class))).thenReturn(updatedAttendeeDTO);

        ResultActions resultActions = attendeeTestHelper.performUpdateAttendee(attendeeIdToUpdate, updateRequest);

        attendeeTestHelper.assertAttendeeResponse(resultActions, updatedAttendeeDTO);

        verify(attendeeService).updateAttendee(eq(attendeeIdToUpdate), any(UpdateAttendeeRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendeeById_whenNameIsNullInRequest_shouldUpdateOtherFieldsAndKeepExistingName() throws Exception {
        Long attendeeIdToUpdate = attendeeDTO1.id();
        String existingName = attendeeDTO1.name();

        UpdateAttendeeRequestDTO requestWithNameNull = new UpdateAttendeeRequestDTO(
                null,
                updateRequest.email(),
                updateRequest.password(),
                updateRequest.role(),
                updateRequest.workingStartTime(),
                updateRequest.workingEndTime()
        );

        AttendeeDTO expectedResponse = new AttendeeDTO(
                attendeeIdToUpdate,
                existingName,
                requestWithNameNull.email()
        );

        when(attendeeService.updateAttendee(eq(attendeeIdToUpdate), any(UpdateAttendeeRequestDTO.class))).thenReturn(expectedResponse);

        ResultActions resultActions = attendeeTestHelper.performUpdateAttendee(attendeeIdToUpdate, requestWithNameNull);

        attendeeTestHelper.assertAttendeeResponse(resultActions, expectedResponse);

        ArgumentCaptor<UpdateAttendeeRequestDTO> dtoCaptor = ArgumentCaptor.forClass(UpdateAttendeeRequestDTO.class);
        verify(attendeeService).updateAttendee(eq(attendeeIdToUpdate), dtoCaptor.capture());
        assertNull(dtoCaptor.getValue().name(), "Name in the DTO passed to service should be null.");
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendeeById_whenEmailIsNullInRequest_shouldUpdateOtherFieldsAndKeepExistingEmail() throws Exception {
        Long attendeeIdToUpdate = attendeeDTO1.id();
        String existingEmail = attendeeDTO1.email();

        UpdateAttendeeRequestDTO requestWithEmailNull = new UpdateAttendeeRequestDTO(
                updateRequest.name(),
                null,
                updateRequest.password(),
                updateRequest.role(),
                updateRequest.workingStartTime(),
                updateRequest.workingEndTime()
        );

        AttendeeDTO expectedResponse = new AttendeeDTO(
                attendeeIdToUpdate,
                requestWithEmailNull.name(),
                existingEmail
        );

        when(attendeeService.updateAttendee(eq(attendeeIdToUpdate), any(UpdateAttendeeRequestDTO.class))).thenReturn(expectedResponse);

        ResultActions resultActions = attendeeTestHelper.performUpdateAttendee(attendeeIdToUpdate, requestWithEmailNull);

        attendeeTestHelper.assertAttendeeResponse(resultActions, expectedResponse);

        ArgumentCaptor<UpdateAttendeeRequestDTO> dtoCaptor = ArgumentCaptor.forClass(UpdateAttendeeRequestDTO.class);
        verify(attendeeService).updateAttendee(eq(attendeeIdToUpdate), dtoCaptor.capture());
        assertNull(dtoCaptor.getValue().email(), "Email in the DTO passed to service should be null.");
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidUpdateRequestProvider")
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendee_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            UpdateAttendeeRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {
        Long attendeeIdToUpdate = attendeeDTO1.id();

        ResultActions resultActions = attendeeTestHelper.performUpdateAttendee(attendeeIdToUpdate, invalidRequest);

        attendeeTestHelper.assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(attendeeService, never()).updateAttendee(anyLong(), any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendeeById_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;
        when(attendeeService.updateAttendee(eq(nonExistentAttendeeId), any(UpdateAttendeeRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = attendeeTestHelper.performUpdateAttendee(nonExistentAttendeeId, updateRequest);

        attendeeTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(attendeeService).updateAttendee(eq(nonExistentAttendeeId), any(UpdateAttendeeRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendeeById_whenServiceThrowsDataIntegrityException_shouldReturn409Conflict() throws Exception {
        Long attendeeIdToUpdate = attendeeDTO1.id();
        String expectedErrorMessage = "Database constraint violation occurred.";
        when(attendeeService.updateAttendee(eq(attendeeIdToUpdate), any(UpdateAttendeeRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException(expectedErrorMessage));

        ResultActions resultActions = attendeeTestHelper.performUpdateAttendee(attendeeIdToUpdate, updateRequest);

        attendeeTestHelper.assertConflictError(resultActions, expectedErrorMessage);

        verify(attendeeService).updateAttendee(eq(attendeeIdToUpdate), any(UpdateAttendeeRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendeeById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(put("/api/attendees/{id}", invalidId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(objectMapper.writeValueAsString(updateRequest)));

        attendeeTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendee_whenWorkingTimeIsMalformedString_shouldReturn400BadRequest() throws Exception {
        Long attendeeIdToUpdate = attendeeDTO1.id();
        String malformedJsonRequest = """
                {
                    "workingStartTime": "INVALID-TIME-FORMAT",
                    "workingEndTime": "17:00"
                }
                """;
        ResultActions resultActions = mockMvc.perform(put("/api/attendees/{id}", attendeeIdToUpdate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        attendeeTestHelper.assertMalformedRequestError(resultActions);

        verify(attendeeService, never()).updateAttendee(anyLong(), any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void updateAttendee_whenRoleIsInvalid_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "role": "INVALID_ROLE",
                }
                """;
        ResultActions resultActions = mockMvc.perform(post("/api/attendees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        attendeeTestHelper.assertMalformedRequestError(resultActions);

        verify(attendeeService, never()).createAttendee(any());
    }

    // === END UPDATE ===

    // === DELETE ===

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void deleteAttendeeById_whenAttendeeExistsAndNotInUse_shouldReturn204NoContent() throws Exception {
        Long attendeeIdToDelete = attendeeDTO1.id();
        doNothing().when(attendeeService).deleteAttendee(attendeeIdToDelete);

        ResultActions resultActions = attendeeTestHelper.performDeleteAttendee(attendeeIdToDelete);

        resultActions
                .andExpect(status().isNoContent());

        verify(attendeeService).deleteAttendee(attendeeIdToDelete);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void deleteAttendeeById_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        doThrow(new EntityNotFoundException(expectedErrorMessage)).when(attendeeService).deleteAttendee(nonExistentAttendeeId);

        ResultActions resultActions = attendeeTestHelper.performDeleteAttendee(nonExistentAttendeeId);

        attendeeTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(attendeeService).deleteAttendee(nonExistentAttendeeId);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void deleteAttendeeById_whenAttendeeInUse_shouldReturn409Conflict() throws Exception {
        Long attendeeIdToDelete = attendeeDTO1.id();
        List<Long> conflictingMeetingIds = List.of(101L, 102L);
        String conflictErrorMessage = String.format("Attendee cannot be deleted because it is used in %d meeting(s). See details.", conflictingMeetingIds.size());
        String conflictIdsMessage = "Conflicting Resource Ids: " +
                conflictingMeetingIds.stream().map(String::valueOf).collect(Collectors.joining(", "));

        doThrow(new ResourceInUseException(conflictErrorMessage, conflictingMeetingIds)).when(attendeeService).deleteAttendee(attendeeIdToDelete);

        ResultActions resultActions = attendeeTestHelper.performDeleteAttendee(attendeeIdToDelete);

        resultActions
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.CONFLICT.value())))
                .andExpect(jsonPath("$.error", is(HttpStatus.CONFLICT.getReasonPhrase())))
                .andExpect(jsonPath("$.messages[0]", is(conflictErrorMessage)))
                .andExpect(jsonPath("$.messages[1]", is(conflictIdsMessage)));

        verify(attendeeService).deleteAttendee(attendeeIdToDelete);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void deleteAttendeeById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: '" + invalidId + "'.";

        ResultActions resultActions = mockMvc.perform(delete("/api/attendees/{id}", invalidId));

        attendeeTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(attendeeService, never()).deleteAttendee(anyLong());
    }

    // === END DELETE ===

    // === AVAILABILITY ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeAvailability_whenAttendeeExistsAndValidDate_shouldReturn200OkAndListOfAvailableSlots() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        LocalDate date = DEFAULT_DATE;

        DateTimeFormatter expectedJsonFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<AvailableSlotDTO> expectedSlots = List.of(
                new AvailableSlotDTO(date.atTime(10, 0), date.atTime(11, 0)),
                new AvailableSlotDTO(date.atTime(11, 30), date.atTime(12, 30))
        );

        when(availabilityService.getAvailableTimeForAttendee(attendeeId, date)).thenReturn(expectedSlots);

        ResultActions resultActions = attendeeTestHelper.performGetAttendeeAvailability(attendeeId, date);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(expectedSlots.size())))
                .andExpect(jsonPath("$[0].startTime", is(expectedSlots.get(0).startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[0].endTime", is(expectedSlots.get(0).endTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].startTime", is(expectedSlots.get(1).startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].endTime", is(expectedSlots.get(1).endTime().format(expectedJsonFormat))));

        verify(availabilityService).getAvailableTimeForAttendee(attendeeId, date);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeAvailability_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = 0L;
        LocalDate date = DEFAULT_DATE;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(availabilityService.getAvailableTimeForAttendee(nonExistentAttendeeId, date))
                .thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = attendeeTestHelper.performGetAttendeeAvailability(nonExistentAttendeeId, date);

        attendeeTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(availabilityService).getAvailableTimeForAttendee(nonExistentAttendeeId, date);

    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeAvailability_whenMissingDateParam_shouldReturn400BadRequest() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        String expectedErrorMessage = "Required parameter 'date' of type 'LocalDate' is missing.";

        ResultActions resultActions = mockMvc.perform(get("/api/attendees/{id}/availability", attendeeId)
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is("Missing Request Parameter")))
                .andExpect(jsonPath("$.messages[0]", is(expectedErrorMessage)));

        verify(availabilityService, never()).getAvailableTimeForAttendee(anyLong(), any(LocalDate.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAttendeeAvailability_whenInvalidDateFormat_shouldReturn400BadRequest() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        String invalidDateStr = "15-08-2024";
        String expectedErrorMessage = "Parameter 'date' should be of type 'LocalDate' but received value: '" + invalidDateStr + "'.";

        ResultActions resultActions = mockMvc.perform(get("/api/attendees/{id}/availability", attendeeId)
                .param("date", invalidDateStr)
                .accept(MediaType.APPLICATION_JSON));

        attendeeTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(availabilityService, never()).getAvailableTimeForAttendee(anyLong(), any(LocalDate.class));
    }

    // === END AVAILABILITY ===

    // === COMMON AVAILABILITY ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void findCommonAttendeeAvailability_whenValidRequest_shouldReturn200OkAndCommonSlots() throws Exception {
        LocalDate date = DEFAULT_DATE;
        Set<Long> attendeeIds = Set.of(attendeeDTO1.id(), attendeeDTO2.id());
        CommonAvailabilityRequestDTO requestDTO = new CommonAvailabilityRequestDTO(
                attendeeIds,
                date
        );

        DateTimeFormatter expectedJsonFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<AvailableSlotDTO> expectedSlots = List.of(
                new AvailableSlotDTO(date.atTime(10, 0), date.atTime(11, 0)),
                new AvailableSlotDTO(date.atTime(11, 30), date.atTime(12, 30))
        );

        when(availabilityService.getCommonAttendeeAvailability(requestDTO)).thenReturn(expectedSlots);

        ResultActions resultActions = attendeeTestHelper.performFindCommonAvailability(requestDTO);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(expectedSlots.size())))
                .andExpect(jsonPath("$[0].startTime", is(expectedSlots.get(0).startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[0].endTime", is(expectedSlots.get(0).endTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].startTime", is(expectedSlots.get(1).startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].endTime", is(expectedSlots.get(1).endTime().format(expectedJsonFormat))));

        verify(availabilityService).getCommonAttendeeAvailability(requestDTO);
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidCommonAvailabilityRequestProvider")
    @WithMockUser(authorities = {"USER"})
    void findCommonAttendeeAvailability_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            CommonAvailabilityRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {

        ResultActions resultActions = attendeeTestHelper.performFindCommonAvailability(invalidRequest);

        attendeeTestHelper.assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(availabilityService, never()).getCommonAttendeeAvailability(any());
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void findCommonAttendeeAvailability_whenInvalidDateFormatInRequest_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "attendeeIds": "[1L, 2L]",
                    "date": "15-08-2024" // Invalid format (DD-MM-YYYY instead of YYYY-MM-DD)
                }
                """;

        ResultActions resultActions = mockMvc.perform(post("/api/attendees/common-availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest)
                .accept(MediaType.APPLICATION_JSON));

        attendeeTestHelper.assertMalformedRequestError(resultActions);

        verify(availabilityService, never()).getCommonAttendeeAvailability(any());
    }

    // === END COMMON AVAILABILITY ===

}
