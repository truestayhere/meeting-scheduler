package com.truestayhere.meeting_scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.config.CustomAuthenticationEntryPoint;
import com.truestayhere.meeting_scheduler.config.SecurityConfig;
import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
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
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private MeetingTestHelper meetingTestHelper;

    LocalDate DEFAULT_DATE = LocalDate.of(Year.now().getValue() + 1, 8, 14);
    LocalDateTime DEFAULT_RANGE_START = DEFAULT_DATE.atTime(10, 0);
    LocalDateTime DEFAULT_RANGE_END = DEFAULT_DATE.atTime(11, 0);

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

    // === CREATE ===

    @Test
    @WithMockUser
    void createMeeting_whenValidInput_shouldReturn201CreatedAndMeetingResponse() throws Exception {
        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class))).thenReturn(meetingDTO1);

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertCreatedMeetingResponse(resultActions, meetingDTO1);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    static Stream<Arguments> invalidCreateRequestProvider() {

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

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidCreateRequestProvider")
    @WithMockUser
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
    @WithMockUser
    void createMeeting_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = createRequest.locationId();
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser
    void createMeeting_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentAttendeeId = attendeeDTO1.id();
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser
    void createMeeting_whenMeetingConflict_shouldReturn409Conflict() throws Exception {
        String expectedErrorMessage = "Database constraint violation occurred.";
        when(meetingService.createMeeting(any(CreateMeetingRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performCreateMeeting(createRequest);

        meetingTestHelper.assertConflictError(resultActions, expectedErrorMessage);

        verify(meetingService).createMeeting(any(CreateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser
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
    @WithMockUser
    void getAllMeetings_shouldReturn200OkAndListOfMeetings() throws Exception {
        List<MeetingDTO> expectedMeetings = List.of(meetingDTO1, meetingDTO2);

        when(meetingService.getAllMeetings()).thenReturn(expectedMeetings);

        ResultActions resultActions = meetingTestHelper.performGetAllMeetings();

        meetingTestHelper.assertMeetingListResponse(resultActions, expectedMeetings);

        verify(meetingService).getAllMeetings();
    }

    @Test
    @WithMockUser
    void getAllMeetings_whenNoMeetings_shouldReturn200OkAndEmptyList() throws Exception {
        List<MeetingDTO> expectedMeetings = List.of();

        when(meetingService.getAllMeetings()).thenReturn(expectedMeetings);

        ResultActions resultActions = meetingTestHelper.performGetAllMeetings();

        meetingTestHelper.assertMeetingListResponse(resultActions, expectedMeetings);

        verify(meetingService).getAllMeetings();
    }

    @Test
    @WithMockUser
    void getMeetingById_whenMeetingExists_shouldReturn200OkAndMeetingResponse() throws Exception {
        Long meetingId = meetingDTO1.id();
        when(meetingService.getMeetingById(meetingId)).thenReturn(meetingDTO1);

        ResultActions resultActions = meetingTestHelper.performGetMeeting(meetingId);

        meetingTestHelper.assertMeetingResponse(resultActions, meetingDTO1);

        verify(meetingService).getMeetingById(meetingId);
    }

    @Test
    @WithMockUser
    void getMeetingById_whenMeetingNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;

        when(meetingService.getMeetingById(nonExistentMeetingId)).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performGetMeeting(nonExistentMeetingId);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).getMeetingById(nonExistentMeetingId);
    }

    @Test
    @WithMockUser
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
    @WithMockUser
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

    static Stream<Arguments> partialUpdateMeetingRequestProvider() {

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

    @ParameterizedTest(name = "Partial Update: {0}")
    @MethodSource("partialUpdateMeetingRequestProvider")
    @WithMockUser
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

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(meetingIdToUpdate, requestDto).andDo(print()); ;

        meetingTestHelper.assertMeetingResponse(resultActions, partiallyUpdatedMeetingDTO);

        verify(meetingService).updateMeeting(eq(meetingIdToUpdate), any(UpdateMeetingRequestDTO.class));
    }

    static Stream<Arguments> invalidUpdateRequestProvider() {

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

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidUpdateRequestProvider")
    @WithMockUser
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
    @WithMockUser
    void updateMeetingById_whenMeetingNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;
        when(meetingService.updateMeeting(eq(nonExistentMeetingId), any(UpdateMeetingRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = meetingTestHelper.performUpdateMeeting(nonExistentMeetingId, updateRequest);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).updateMeeting(eq(nonExistentMeetingId), any(UpdateMeetingRequestDTO.class));
    }

    @Test
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
    void deleteMeetingById_whenMeetingExists_shouldReturn204NoContent() throws Exception {
        Long meetingIdToDelete = attendeeDTO1.id();
        doNothing().when(meetingService).deleteMeeting(meetingIdToDelete);

        ResultActions resultActions = meetingTestHelper.performDeleteMeeting(meetingIdToDelete);

        resultActions
                .andExpect(status().isNoContent());

        verify(meetingService).deleteMeeting(meetingIdToDelete);
    }

    @Test
    @WithMockUser
    void deleteMeetingById_whenMeetingNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;
        doThrow(new EntityNotFoundException(expectedErrorMessage)).when(meetingService).deleteMeeting(nonExistentMeetingId);

        ResultActions resultActions = meetingTestHelper.performDeleteMeeting(nonExistentMeetingId);

        meetingTestHelper.assertNotFoundError(resultActions, expectedErrorMessage);

        verify(meetingService).deleteMeeting(nonExistentMeetingId);
    }

    @Test
    @WithMockUser
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
    @WithMockUser
    void getMeetingsByAttendeeAndRange_whenValidInputAndAttendeeExists_shouldReturn200OkAndListOfMeetings() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByAttendeeAndRange_whenAttendeeNotFound_shouldReturn404NotFound() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByAttendeeAndRange_whenMissingTimeParams_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByAttendeeAndRange_whenInvalidDateTimeFormat_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByAttendeeAndRange_whenStartTimeAfterEndTime_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByAttendeeAndRange_whenAttendeeIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception{

    }

    // === END GET BY ATTENDEE ===

    // === GET BY LOCATION ===

    @Test
    @WithMockUser
    void getMeetingsByLocationAndRange_whenValidInputAndLocationExists_shouldReturn200OkAndListOfMeetings() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByLocationAndRange_whenLocationNotFound_shouldReturn404NotFound() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByLocationAndRange_whenMissingTimeParams_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByLocationAndRange_whenInvalidDateTimeFormat_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByLocationAndRange_whenStartTimeAfterEndTime_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void getMeetingsByLocationAndRange_whenLocationIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception{

    }

    // === END GET BY LOCATION ===

    // === MEETING SUGGESTIONS ===

    @Test
    @WithMockUser
    void findMeetingSuggestions_whenValidRequest_shouldReturn200OkAndListOfSuggestions() throws Exception{

    }

    static Stream<Arguments> invalidMeetingSuggestionsRequestProvider() {

        /**
         * attendeeIds is null or empty
         *
         * date is null
         *
         * durationMinutes is null
         *
         * durationMinutes less than min
         *
         * minimum capacity is less than min
         */

        return Stream.of(
                Arguments.of()
        );
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidMeetingSuggestionsRequestProvider")
    @WithMockUser
    void findMeetingSuggestions_whenInvalidInput_shouldReturn400BadRequest() throws Exception{

    }

    @Test
    @WithMockUser
    void findMeetingSuggestions_whenAnyAttendeeOrLocationNotFound_shouldReturn404NotFoundOrSpecificError() throws Exception{

    }

    // === END MEETING SUGGESTIONS ===

}
