package com.truestayhere.meeting_scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.config.CustomAuthenticationEntryPoint;
import com.truestayhere.meeting_scheduler.config.SecurityConfig;
import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.MeetingSuggestionRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.*;
import com.truestayhere.meeting_scheduler.exception.GlobalExceptionHandler;
import com.truestayhere.meeting_scheduler.helper.MeetingTestHelper;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import com.truestayhere.meeting_scheduler.service.MeetingService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(MeetingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class MeetingControllerTest {

    @MockitoBean
    MeetingService meetingService;
    @MockitoBean
    AvailabilityService availabilityService;
    @MockitoBean
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    UserDetailsService userDetailsService;
    LocalDate DEFAULT_DATE = LocalDate.of(Year.now().getValue() + 1, 8, 14);
    LocalDateTime DEFAULT_RANGE_START = DEFAULT_DATE.atTime(10, 0);
    LocalDateTime DEFAULT_RANGE_END = DEFAULT_DATE.atTime(11, 0);
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private MeetingTestHelper meetingTestHelper;
    private CreateMeetingRequestDTO createRequest;
    private UpdateMeetingRequestDTO updateRequest;
    private MeetingDTO meetingDTO1, meetingDTO2;
    private AttendeeDTO attendeeDTO1, attendeeDTO2;
    private LocationDTO locationDTO1, locationDTO2;

    @BeforeEach
    void setUp() {
        meetingTestHelper = new MeetingTestHelper(mockMvc, objectMapper);

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

        locationDTO1 = new LocationDTO(
                201L,
                "Location One",
                10
        );

        locationDTO2 = new LocationDTO(
                202L,
                "Location Two",
                20
        );

        meetingDTO1 = new MeetingDTO(
                101L,
                "Meeting One",
                DEFAULT_RANGE_START,
                DEFAULT_RANGE_END,
                locationDTO1,
                Set.of(attendeeDTO1, attendeeDTO2)
        );

        meetingDTO2 = new MeetingDTO(
                102L,
                "MeetingTwo",
                DEFAULT_RANGE_START.plusHours(1),
                DEFAULT_RANGE_END.plusHours(1),
                locationDTO2,
                Set.of(attendeeDTO1)
        );

        createRequest = new CreateMeetingRequestDTO(
                meetingDTO1.title(),
                meetingDTO1.startTime(),
                meetingDTO1.endTime(),
                meetingDTO1.location().id(),
                Set.of(attendeeDTO1.id(), attendeeDTO2.id())
        );

        updateRequest = new UpdateMeetingRequestDTO(
                createRequest.title() + " Updated",
                createRequest.startTime().minusHours(1),
                createRequest.endTime().minusHours(1),
                locationDTO2.id(),
                Set.of(attendeeDTO2.id())
        );

    }

    private static Stream<Arguments> invalidCreateRequestProvider() {

        String validTitle = "Meeting Title";
        String longTitle = "a".repeat(201);
        LocalDate validDate = LocalDate.of(Year.now().getValue() + 1, 8, 14);
        LocalDate pastDate = LocalDate.of(Year.now().getValue() - 100, 8, 14);
        LocalDateTime exampleStartTime = validDate.atTime(10, 0);
        LocalDateTime pastStartTime = pastDate.atTime(10, 0);
        LocalDateTime exampleEndTime = validDate.atTime(11, 0);
        Long validLocationId = 201L;
        Set<Long> validAttendeeIds = Set.of(101L, 102L);

        return Stream.of(
                Arguments.of(
                        "Title is blank",
                        new CreateMeetingRequestDTO(null, exampleStartTime, exampleEndTime, validLocationId, validAttendeeIds),
                        "title",
                        "Meeting title must not be blank."
                ),
                Arguments.of(
                        "Title is longer than max",
                        new CreateMeetingRequestDTO(longTitle, exampleStartTime, exampleEndTime, validLocationId, validAttendeeIds),
                        "title",
                        "Meeting title must not exceed 200 characters."
                ),
                Arguments.of(
                        "StartTime is null",
                        new CreateMeetingRequestDTO(validTitle, null, exampleEndTime, validLocationId, validAttendeeIds),
                        "startTime",
                        "Meeting start time cannot be null."
                ),
                Arguments.of(
                        "StartTime in the past",
                        new CreateMeetingRequestDTO(validTitle, pastStartTime, exampleEndTime, validLocationId, validAttendeeIds),
                        "startTime",
                        "Meeting start time cannot be set in the past."
                ),
                Arguments.of(
                        "EndTime is null",
                        new CreateMeetingRequestDTO(validTitle, exampleStartTime, null, validLocationId, validAttendeeIds),
                        "endTime",
                        "Meeting end time cannot be null."
                ),
                Arguments.of(
                        "LocationId is null",
                        new CreateMeetingRequestDTO(validTitle, exampleStartTime, exampleEndTime, null, validAttendeeIds),
                        "locationId",
                        "Meeting location ID cannot be null."
                ),
                Arguments.of(
                        "AttendeeIds is null or empty",
                        new CreateMeetingRequestDTO(validTitle, exampleStartTime, exampleEndTime, validLocationId, null),
                        "attendeeIds",
                        "Attendee list cannot be empty."
                ),
                Arguments.of(
                        "StartTime is not before endTime",
                        new CreateMeetingRequestDTO(validTitle, exampleEndTime, exampleStartTime, validLocationId, validAttendeeIds),
                        "createMeetingRequestDTO",
                        "Start time must me before end time."
                )
        );
    }

    private static Stream<Arguments> partialUpdateMeetingRequestProvider() {

        String newTitle = "Updated Title";
        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 14);
        LocalDateTime newStartTime = date.atTime(10, 15);
        LocalDateTime newEndTime = date.atTime(11, 15);
        Long newLocationId = 202L;
        Set<Long> newAttendeeIds = Set.of(2L);

        return Stream.of(
                Arguments.of(
                        "Title Updated",
                        new UpdateMeetingRequestDTO(newTitle, null, null, null, null)
                ),
                Arguments.of(
                        "Start time Updated",
                        new UpdateMeetingRequestDTO(null, newStartTime, null, null, null)
                ),
                Arguments.of(
                        "End time Updated",
                        new UpdateMeetingRequestDTO(null, null, newEndTime, null, null)
                ),
                Arguments.of(
                        "LocationId Updated",
                        new UpdateMeetingRequestDTO(null, null, null, newLocationId, null)
                ),
                Arguments.of(
                        "AttendeeIds Updated",
                        new UpdateMeetingRequestDTO(null, null, null, null, newAttendeeIds)
                )
        );
    }

    private static Stream<Arguments> invalidUpdateRequestProvider() {

        String validTitle = "Meeting Title Updated";
        String longTitle = "a".repeat(201);
        LocalDate validDate = LocalDate.of(Year.now().getValue() + 1, 8, 14);
        LocalDate pastDate = LocalDate.of(Year.now().getValue() - 100, 8, 14);
        LocalDateTime exampleStartTime = validDate.atTime(10, 15);
        LocalDateTime pastStartTime = pastDate.atTime(10, 15);
        LocalDateTime exampleEndTime = validDate.atTime(11, 15);
        Long validLocationId = 202L;
        Set<Long> validAttendeeIds = Set.of(102L);

        return Stream.of(
                Arguments.of(
                        "Title is blank",
                        new UpdateMeetingRequestDTO("", exampleStartTime, exampleEndTime, validLocationId, validAttendeeIds),
                        "updateMeetingRequestDTO",
                        "Title cannot be blank when provided."
                ),
                Arguments.of(
                        "Title is longer than max",
                        new UpdateMeetingRequestDTO(longTitle, exampleStartTime, exampleEndTime, validLocationId, validAttendeeIds),
                        "title",
                        "Meeting title must not exceed 200 characters."
                ),
                Arguments.of(
                        "AttendeeIds is empty",
                        new UpdateMeetingRequestDTO(validTitle, exampleStartTime, exampleEndTime, validLocationId, Set.of()),
                        "updateMeetingRequestDTO",
                        "Attendee list cannot be empty when provided."
                ),
                Arguments.of(
                        "StartTime in the past",
                        new UpdateMeetingRequestDTO(validTitle, pastStartTime, exampleEndTime, validLocationId, validAttendeeIds),
                        "startTime",
                        "Meeting start time cannot be set in the past."
                )
        );
    }

    private static Stream<Arguments> invalidMeetingSuggestionsRequestProvider() {

        Set<Long> validAttendeeIds = Set.of(1L, 2L);
        Integer validDurationMinutes = 30;
        LocalDate validDate = LocalDate.of(Year.now().getValue() + 1, 8, 14);

        return Stream.of(
                Arguments.of(
                        "AttendeeIds is null or empty",
                        new MeetingSuggestionRequestDTO(null, validDurationMinutes, validDate),
                        "attendeeIds",
                        "Attendee list cannot be empty."
                ),
                Arguments.of(
                        "Date is null",
                        new MeetingSuggestionRequestDTO(validAttendeeIds, validDurationMinutes, null),
                        "date",
                        "A date must be provided."
                ),
                Arguments.of(
                        "DurationMinutes is null",
                        new MeetingSuggestionRequestDTO(validAttendeeIds, null, validDate),
                        "durationMinutes",
                        "Meeting duration cannot be empty."
                ),
                Arguments.of(
                        "DurationMinutes less than min",
                        new MeetingSuggestionRequestDTO(validAttendeeIds, 0, validDate),
                        "durationMinutes",
                        "Duration must me at least 1 minute."
                )
        );
    }

    // === CREATE ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void createMeeting_whenValidInput_shouldReturn201CreatedAndMeetingResponse() throws Exception {
        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class))).thenReturn(meetingDTO1);

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertCreatedMeetingResponse(resultActions, meetingDTO1);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidCreateRequestProvider")
    @WithMockUser(authorities = {"USER"})
    void createMeeting_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            CreateMeetingRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(invalidRequest);

        meetingTestHelper.assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(meetingService, never()).createMeeting(any());
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void createMeeting_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = createRequest.locationId();
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void createMeeting_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = attendeeDTO1.id();
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void createMeeting_whenMeetingConflict_shouldReturn409Conflict() throws Exception {
        String expectedErrorMessage = "Database constraint violation occurred.";
        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertConflictError(resultActions, expectedErrorMessage);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void createMeeting_whenInvalidDateTimeFormatInRequest_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "title": "Meeting Title",
                    "startTime": "INVALID-TIME-FORMAT",
                    "endTime": "INVALID-TIME-FORMAT",
                    "locationId": "201L",
                    "attendeeIds": "[1L, 2L]"
                }
                """;
        ResultActions resultActions = mockMvc.perform(post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        meetingTestHelper.assertMalformedRequestError(resultActions);

        verify(meetingService, never()).createMeeting(any());
    }

    // === END CREATE ===

    // === GET ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAllMeetings_shouldReturn200OkAndListOfMeetings() throws Exception {
        List<MeetingDTO> expectedMeetings = List.of(meetingDTO1, meetingDTO2);

        when(meetingService.getAllMeetings()).thenReturn(expectedMeetings);

        ResultActions resultActions = meetingTestHelper.performGetAllMeetings();

        meetingTestHelper.assertMeetingListResponse(resultActions, expectedMeetings);

        verify(meetingService).getAllMeetings();
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getAllMeetings_whenNoMeetings_shouldReturn200OkAndEmptyList() throws Exception {
        List<MeetingDTO> expectedMeetings = List.of();

        when(meetingService.getAllMeetings()).thenReturn(expectedMeetings);

        ResultActions resultActions = meetingTestHelper.performGetAllMeetings();

        meetingTestHelper.assertMeetingListResponse(resultActions, expectedMeetings);

        verify(meetingService).getAllMeetings();
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingById_whenMeetingExists_shouldReturn200OkAndMeetingResponse() throws Exception {
        Long meetingId = meetingDTO1.id();
        when(meetingService.getMeetingById(meetingId)).thenReturn(meetingDTO1);

        ResultActions resultActions = meetingTestHelper.performGetMeeting(meetingId);

        meetingTestHelper.assertMeetingResponse(resultActions, meetingDTO1);

        verify(meetingService).getMeetingById(meetingId);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingById_whenMeetingNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;

        when(meetingService.getMeetingById(nonExistentMeetingId)).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performGetMeeting(nonExistentMeetingId);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).getMeetingById(nonExistentMeetingId);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/{id}", invalidId)
                .accept(MediaType.APPLICATION_JSON));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);
    }

    // === END GET ===

    // === UPDATE ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenValidInputAndMeetingExists_shouldReturn200OkAndUpdatedMeetingResponse() throws Exception {
        Long meetingIdToUpdate = meetingDTO1.id();

        MeetingDTO updatedMeetingDTO = new MeetingDTO(
                meetingIdToUpdate,
                updateRequest.title(),
                updateRequest.startTime(),
                updateRequest.endTime(),
                locationDTO1,
                Set.of(attendeeDTO1, attendeeDTO2)
        );

        when(meetingService.updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class))).thenReturn(updatedMeetingDTO);

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, updateRequest);

        meetingTestHelper.assertMeetingResponse(resultActions, updatedMeetingDTO);

        verify(meetingService).updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class));
    }

    @ParameterizedTest(name = "Partial Update: {0}")
    @MethodSource("partialUpdateMeetingRequestProvider")
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenPartialDataProvided_shouldCallServiceWithCorrectPartialDTO(
            String testCaseDescription,
            UpdateMeetingRequestDTO requestDto) throws Exception {

        Long meetingIdToUpdate = meetingDTO1.id();

        MeetingDTO partiallyUpdatedMeetingDTO = new MeetingDTO(
                meetingIdToUpdate,
                requestDto.title() != null ? requestDto.title() : meetingDTO1.title(),
                requestDto.startTime() != null ? requestDto.startTime() : meetingDTO1.startTime(),
                requestDto.endTime() != null ? requestDto.endTime() : meetingDTO1.endTime(),
                requestDto.locationId() != null ? locationDTO2 : meetingDTO1.location(),
                requestDto.attendeeIds() != null ? Set.of(attendeeDTO2) : meetingDTO1.attendees()
        );

        when(meetingService.updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class))).thenReturn(partiallyUpdatedMeetingDTO);

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, requestDto).andDo(print());

        meetingTestHelper.assertMeetingResponse(resultActions, partiallyUpdatedMeetingDTO);

        verify(meetingService).updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class));
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidUpdateRequestProvider")
    @WithMockUser(authorities = {"USER"})
    void updateMeeting_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            UpdateMeetingRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {
        Long meetingIdToUpdate = meetingDTO1.id();

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, invalidRequest);

        meetingTestHelper.assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(meetingService, never()).updateMeeting(anyLong(), any());
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenMeetingNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;
        when(meetingService.updateMeeting(eq(nonExistentMeetingId), any(UpdateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(nonExistentMeetingId, updateRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).updateMeeting(eq(nonExistentMeetingId), any(UpdateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long meetingIdToUpdate = meetingDTO1.id();
        Long nonExistentLocationId = 0L;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;
        when(meetingService.updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, updateRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long meetingIdToUpdate = meetingDTO1.id();
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;
        when(meetingService.updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, updateRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenMeetingConflict_shouldReturn409Conflict() throws Exception {
        Long meetingIdToUpdate = meetingDTO1.id();
        String expectedErrorMessage = "Database constraint violation occurred.";
        when(meetingService.updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, updateRequest);

        meetingTestHelper.assertConflictError(resultActions, expectedErrorMessage);

        verify(meetingService).updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(put("/api/meetings/{id}", invalidId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(objectMapper.writeValueAsString(updateRequest)));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(meetingService, never()).deleteMeeting(anyLong());
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void updateMeetingById_whenInvalidDateTimeFormatInRequest_shouldReturn400BadRequest() throws Exception {
        Long meetingIdToUpdate = meetingDTO1.id();
        String malformedJsonRequest = """
                {
                    "title": "Meeting Title Updated",
                    "startTime": "INVALID-TIME-FORMAT",
                    "endTime": "INVALID-TIME-FORMAT",
                    "locationId": "202L",
                    "attendeeIds": "[2L]"
                }
                """;

        ResultActions resultActions = mockMvc.perform(put("/api/meetings/{id}", meetingIdToUpdate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        meetingTestHelper.assertMalformedRequestError(resultActions);

        verify(meetingService, never()).updateMeeting(anyLong(), any());
    }

    // === END UPDATE ===

    // === DELETE ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void deleteMeetingById_whenMeetingExists_shouldReturn204NoContent() throws Exception {
        Long meetingIdToDelete = attendeeDTO1.id();
        doNothing().when(meetingService).deleteMeeting(meetingIdToDelete);

        ResultActions resultActions = meetingTestHelper.performDeleteMeeting(meetingIdToDelete);

        resultActions
                .andExpect(status().isNoContent());

        verify(meetingService).deleteMeeting(meetingIdToDelete);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void deleteMeetingById_whenMeetingNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;
        doThrow(new EntityNotFoundException(expectedErrorMessage)).when(meetingService).deleteMeeting(nonExistentMeetingId);

        ResultActions resultActions = meetingTestHelper.performDeleteMeeting(nonExistentMeetingId);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).deleteMeeting(nonExistentMeetingId);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void deleteMeetingById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(delete("/api/meetings/{id}", invalidId));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(meetingService, never()).deleteMeeting(anyLong());
    }

    // === END DELETE ===

    // === GET BY ATTENDEE ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByAttendeeAndRange_whenValidInputAndAttendeeExists_shouldReturn200OkAndListOfMeetings() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        LocalDateTime rangeStart = DEFAULT_RANGE_START;
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        List<MeetingDTO> expectedResults = List.of(meetingDTO1, meetingDTO2);
        when(availabilityService.getMeetingsForAttendeeInRange(attendeeId, rangeStart, rangeEnd)).thenReturn(expectedResults);

        ResultActions resultActions = meetingTestHelper.performGetMeetingsByAttendeeAndRange(attendeeId, rangeStart, rangeEnd);

        meetingTestHelper.assertMeetingListResponse(resultActions, expectedResults);

        verify(availabilityService).getMeetingsForAttendeeInRange(attendeeId, rangeStart, rangeEnd);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByAttendeeAndRange_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = 0L;
        LocalDateTime rangeStart = DEFAULT_RANGE_START;
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;
        when(availabilityService.getMeetingsForAttendeeInRange(nonExistentAttendeeId, rangeStart, rangeEnd)).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performGetMeetingsByAttendeeAndRange(nonExistentAttendeeId, rangeStart, rangeEnd);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(availabilityService).getMeetingsForAttendeeInRange(nonExistentAttendeeId, rangeStart, rangeEnd);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByAttendeeAndRange_whenMissingTimeParams_shouldReturn400BadRequest() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        String expectedErrorMessage = "Required parameter 'start' of type 'LocalDateTime' is missing.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/byAttendee/{id}", attendeeId)
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is("Missing Request Parameter")))
                .andExpect(jsonPath("$.messages[0]", is(expectedErrorMessage)));

        verify(availabilityService, never()).getMeetingsForAttendeeInRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByAttendeeAndRange_whenInvalidDateTimeFormat_shouldReturn400BadRequest() throws Exception {
        Long attendeeId = attendeeDTO1.id();
        String invalidDateTimeStr = "30-08-2025T10:00:00";
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        String expectedErrorMessage = "Parameter 'start' should be of type 'LocalDateTime' but received value: '" + invalidDateTimeStr + "'.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/byAttendee/{id}", attendeeId)
                .param("start", invalidDateTimeStr)
                .param("end", rangeEnd.toString())
                .accept(MediaType.APPLICATION_JSON));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(availabilityService, never()).getMeetingsForAttendeeInRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByAttendeeAndRange_whenAttendeeIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        LocalDateTime rangeStart = DEFAULT_RANGE_START;
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        String expectedErrorMessage = "Parameter 'attendeeId' should be of type 'Long' but received value: '" + invalidId + "'.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/byAttendee/{id}", invalidId)
                .param("start", rangeStart.toString())
                .param("end", rangeEnd.toString())
                .accept(MediaType.APPLICATION_JSON));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(availabilityService, never()).getMeetingsForAttendeeInRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // === END GET BY ATTENDEE ===

    // === GET BY LOCATION ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByLocationAndRange_whenValidInputAndLocationExists_shouldReturn200OkAndListOfMeetings() throws Exception {
        Long locationId = locationDTO1.id();
        LocalDateTime rangeStart = DEFAULT_RANGE_START;
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        List<MeetingDTO> expectedResults = List.of(meetingDTO1, meetingDTO2);
        when(availabilityService.getMeetingsForLocationInRange(locationId, rangeStart, rangeEnd)).thenReturn(expectedResults);

        ResultActions resultActions = meetingTestHelper.performGetMeetingsByLocationAndRange(locationId, rangeStart, rangeEnd);

        meetingTestHelper.assertMeetingListResponse(resultActions, expectedResults);

        verify(availabilityService).getMeetingsForLocationInRange(locationId, rangeStart, rangeEnd);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByLocationAndRange_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = 0L;
        LocalDateTime rangeStart = DEFAULT_RANGE_START;
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;
        when(availabilityService.getMeetingsForLocationInRange(nonExistentLocationId, rangeStart, rangeEnd)).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performGetMeetingsByLocationAndRange(nonExistentLocationId, rangeStart, rangeEnd);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(availabilityService).getMeetingsForLocationInRange(nonExistentLocationId, rangeStart, rangeEnd);
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByLocationAndRange_whenMissingTimeParams_shouldReturn400BadRequest() throws Exception {
        Long locationId = locationDTO1.id();
        String expectedErrorMessage = "Required parameter 'start' of type 'LocalDateTime' is missing.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/byLocation/{id}", locationId)
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is("Missing Request Parameter")))
                .andExpect(jsonPath("$.messages[0]", is(expectedErrorMessage)));

        verify(availabilityService, never()).getMeetingsForLocationInRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByLocationAndRange_whenInvalidDateTimeFormat_shouldReturn400BadRequest() throws Exception {
        Long locationId = locationDTO1.id();
        String invalidDateTimeStr = "30-08-2025T10:00:00";
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        String expectedErrorMessage = "Parameter 'start' should be of type 'LocalDateTime' but received value: '" + invalidDateTimeStr + "'.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/byLocation/{id}", locationId)
                .param("start", invalidDateTimeStr)
                .param("end", rangeEnd.toString())
                .accept(MediaType.APPLICATION_JSON));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(availabilityService, never()).getMeetingsForLocationInRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void getMeetingsByLocationAndRange_whenLocationIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        LocalDateTime rangeStart = DEFAULT_RANGE_START;
        LocalDateTime rangeEnd = DEFAULT_RANGE_END;
        String expectedErrorMessage = "Parameter 'locationId' should be of type 'Long' but received value: '" + invalidId + "'.";

        ResultActions resultActions = mockMvc.perform(get("/api/meetings/byLocation/{id}", invalidId)
                .param("start", rangeStart.toString())
                .param("end", rangeEnd.toString())
                .accept(MediaType.APPLICATION_JSON));

        meetingTestHelper.assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(availabilityService, never()).getMeetingsForLocationInRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // === END GET BY LOCATION ===

    // === MEETING SUGGESTIONS ===

    @Test
    @WithMockUser(authorities = {"USER"})
    void findMeetingSuggestions_whenValidRequest_shouldReturn200OkAndListOfSuggestions() throws Exception {
        Set<Long> attendeeIds = Set.of(attendeeDTO1.id(), attendeeDTO2.id());
        Integer durationMinutes = 30;
        LocalDate date = DEFAULT_DATE;
        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                attendeeIds,
                durationMinutes,
                date
        );

        DateTimeFormatter expectedJsonFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<LocationTimeSlotDTO> expectedResults = List.of(
                new LocationTimeSlotDTO(
                        locationDTO1,
                        new AvailableSlotDTO(date.atTime(10, 0), date.atTime(11, 0))
                ),
                new LocationTimeSlotDTO(
                        locationDTO2,
                        new AvailableSlotDTO(date.atTime(11, 0), date.atTime(12, 30))
                )
        );
        when(availabilityService.findMeetingSuggestions(requestDTO)).thenReturn(expectedResults);

        ResultActions resultActions = meetingTestHelper.performFindMeetingSuggestions(requestDTO);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(expectedResults.size())))
                .andExpect(jsonPath("$[0].location.id", is(expectedResults.get(0).location().id().intValue())))
                .andExpect(jsonPath("$[0].availableSlot.startTime", is(expectedResults.get(0).availableSlot().startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[0].availableSlot.endTime", is(expectedResults.get(0).availableSlot().endTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].location.id", is(expectedResults.get(1).location().id().intValue())))
                .andExpect(jsonPath("$[1].availableSlot.startTime", is(expectedResults.get(1).availableSlot().startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].availableSlot.endTime", is(expectedResults.get(1).availableSlot().endTime().format(expectedJsonFormat))));

        verify(availabilityService).findMeetingSuggestions(requestDTO);
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidMeetingSuggestionsRequestProvider")
    @WithMockUser(authorities = {"USER"})
    void findMeetingSuggestions_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            MeetingSuggestionRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {
        ResultActions resultActions = meetingTestHelper.performFindMeetingSuggestions(invalidRequest);

        meetingTestHelper.assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(availabilityService, never()).findMeetingSuggestions(any());
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void findMeetingSuggestions_whenInvalidDateFormatInRequest_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "attendeeIds": "[1L, 2L]",
                    "durationMinutes": "30",
                    "date": "15-08-2024" // Invalid format (DD-MM-YYYY instead of YYYY-MM-DD)
                }
                """;

        ResultActions resultActions = mockMvc.perform(post("/api/meetings/suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest)
                .accept(MediaType.APPLICATION_JSON));

        meetingTestHelper.assertMalformedRequestError(resultActions);

        verify(availabilityService, never()).findMeetingSuggestions(any());
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void findMeetingSuggestions_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = attendeeDTO1.id();
        Integer durationMinutes = 30;
        LocalDate date = DEFAULT_DATE;
        MeetingSuggestionRequestDTO requestDTOWithNonExistentAttendee = new MeetingSuggestionRequestDTO(
                Set.of(nonExistentAttendeeId),
                durationMinutes,
                date
        );
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;
        when(availabilityService.findMeetingSuggestions(any(MeetingSuggestionRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performFindMeetingSuggestions(requestDTOWithNonExistentAttendee);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(availabilityService).findMeetingSuggestions(any(MeetingSuggestionRequestDTO.class));
    }

    // === END MEETING SUGGESTIONS ===

}
