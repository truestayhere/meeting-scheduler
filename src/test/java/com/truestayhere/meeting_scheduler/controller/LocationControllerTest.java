package com.truestayhere.meeting_scheduler.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.config.CustomAuthenticationEntryPoint;
import com.truestayhere.meeting_scheduler.config.SecurityConfig;
import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.LocationAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AvailableSlotDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationTimeSlotDTO;
import com.truestayhere.meeting_scheduler.exception.GlobalExceptionHandler;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import com.truestayhere.meeting_scheduler.service.LocationService;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(LocationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class LocationControllerTest {

    @MockitoBean
    LocationService locationService;
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

    private CreateLocationRequestDTO createRequest;
    private UpdateLocationRequestDTO updateRequest;
    private LocationDTO locationDTO1, locationDTO2;


    @BeforeEach
    void setUp() {
        locationDTO1 = new LocationDTO(
                1L,
                "Room 1",
                10
        );

        locationDTO2 = new LocationDTO(
                2L,
                "Room 2",
                20
        );

        createRequest = new CreateLocationRequestDTO(
                locationDTO1.name(),
                locationDTO1.capacity(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        updateRequest = new UpdateLocationRequestDTO(
                locationDTO1.name() + " Updated",
                locationDTO1.capacity() + 10,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );
    }

    // === CREATE ===

    @Test
    @WithMockUser
    void createLocation_whenValidInput_shouldReturn201CreatedAndLocationResponse() throws Exception {
        when(locationService.createLocation(any(CreateLocationRequestDTO.class))).thenReturn(locationDTO1);

        ResultActions resultActions = performCreateLocation(createRequest);

        assertCreatedLocationResponse(resultActions, locationDTO1);

        verify(locationService).createLocation(any(CreateLocationRequestDTO.class));
    }


    static Stream<Arguments> invalidCreateRequestProvider() {

        String validName = "Room 1";
        String longName = "a".repeat(151);
        Integer validCapacity = 10;
        LocalTime validStartTime = LocalTime.of(9, 0);
        LocalTime validEndTime = LocalTime.of(17, 0);
        LocalTime sameTime = LocalTime.of(10, 0);

        return Stream.of(
                Arguments.of(
                        "Name is null",
                        new CreateLocationRequestDTO(null, validCapacity, validStartTime, validEndTime),
                        "name",
                        "Location name cannot be blank."
                ),
                Arguments.of(
                        "Name longer than max",
                        new CreateLocationRequestDTO(longName, validCapacity, validStartTime, validEndTime),
                        "name",
                        "Location name cannot exceed 150 characters."
                ),
                Arguments.of(
                        "Capacity is null",
                        new CreateLocationRequestDTO(validName, null, validStartTime, validEndTime),
                        "capacity",
                        "Location capacity cannot be empty."
                ),
                Arguments.of(
                        "Capacity less than min",
                        new CreateLocationRequestDTO(validName, 0, validStartTime, validEndTime),
                        "capacity",
                        "Location capacity must be at least 1."
                ),
                Arguments.of(
                        "Working start time equals end time",
                        new CreateLocationRequestDTO(validName, validCapacity, sameTime, sameTime),
                        "createLocationRequestDTO",
                        "Working start time and end time cannot be the same"
                )
        );
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidCreateRequestProvider")
    @WithMockUser
    void createLocation_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            CreateLocationRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {

        ResultActions resultActions = performCreateLocation(invalidRequest);

        assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(locationService, never()).createLocation(any());
    }

    @Test
    @WithMockUser
    void createLocation_whenWorkingTimeIsMalformedString_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "name": "Test Room",
                    "capacity": 5,
                    "workingStartTime": "INVALID-TIME-FORMAT",
                    "workingEndTime": "17:00"
                }
                """;

        ResultActions resultActions = mockMvc.perform(post("/api/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        assertMalformedRequestError(resultActions);

        verify(locationService, never()).createLocation(any());
    }

    @Test
    @WithMockUser
    void createLocation_whenServiceThrowsDataIntegrityException_shouldReturn409Conflict() throws Exception {
        String expectedErrorMessage = "Database constraint violation occurred.";
        when(locationService.createLocation(any(CreateLocationRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException(expectedErrorMessage));

        ResultActions resultActions = performCreateLocation(createRequest);

        assertConflictError(resultActions, expectedErrorMessage);

        verify(locationService).createLocation(any(CreateLocationRequestDTO.class));
    }

    // === END CREATE ===

    // === GET ===

    @Test
    @WithMockUser
    void getAllLocations_shouldReturn200OkAndListOfLocations() throws Exception {
        List<LocationDTO> expectedLocations = List.of(locationDTO1, locationDTO2);

        when(locationService.getAllLocations()).thenReturn(expectedLocations);

        ResultActions resultActions = performGetAllLocations();

        assertLocationListResponse(resultActions, expectedLocations);

        verify(locationService).getAllLocations();
    }

    @Test
    @WithMockUser
    void getAllLocations_whenNoLocations_shouldReturn200OkAndEmptyList() throws Exception {
        when(locationService.getAllLocations()).thenReturn(List.of());

        ResultActions resultActions = performGetAllLocations();

        assertLocationListResponse(resultActions, List.of());

        verify(locationService).getAllLocations();
    }

    @Test
    @WithMockUser
    void getLocationById_whenLocationExists_shouldReturn200OkAndLocationResponse() throws Exception {
        Long locationId = locationDTO1.id();
        when(locationService.getLocationById(locationId)).thenReturn(locationDTO1);

        ResultActions resultActions = performGetLocation(locationId);

        assertLocationResponse(resultActions, locationDTO1);

        verify(locationService).getLocationById(locationId);
    }

    @Test
    @WithMockUser
    void getLocationById_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = 0L;
        String expectedErrorMessage = "Location not found with id: " + nonExistentLocationId;
        when(locationService.getLocationById(nonExistentLocationId)).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = performGetLocation(nonExistentLocationId);

        assertNotFoundError(resultActions, expectedErrorMessage);

        verify(locationService).getLocationById(nonExistentLocationId);
    }

    @Test
    @WithMockUser
    void getLocationById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(get("/api/locations/{id}", invalidId)
                .accept(MediaType.APPLICATION_JSON));

        assertParameterTypeError(resultActions, expectedErrorMessage);
    }

    // === END GET ===

    // === UPDATE ===

    @Test
    @WithMockUser
    void updateLocationById_whenValidInputAndLocationExists_shouldReturn200OkAndUpdatedLocationResponse() throws Exception {
        Long locationIdToUpdate = locationDTO1.id();

        LocationDTO updatedLocationDTO = new LocationDTO(
                locationIdToUpdate,
                updateRequest.name(),
                updateRequest.capacity()
        );

        when(locationService.updateLocation(eq(locationIdToUpdate), any(UpdateLocationRequestDTO.class)))
                .thenReturn(updatedLocationDTO);

        ResultActions resultActions = performUpdateLocation(locationIdToUpdate, updateRequest);

        assertLocationResponse(resultActions, updatedLocationDTO);

        verify(locationService).updateLocation(eq(locationIdToUpdate), any(UpdateLocationRequestDTO.class));
    }

    static Stream<Arguments> invalidUpdateRequestProvider() {

        String validName = "Room 1";
        String longName = "a".repeat(151);
        Integer validCapacity = 10;
        LocalTime validStartTime = LocalTime.of(9, 0);
        LocalTime validEndTime = LocalTime.of(17, 0);
        LocalTime sameTime = LocalTime.of(10, 0);

        return Stream.of(
                Arguments.of(
                        "Name longer than max",
                        new UpdateLocationRequestDTO(longName, validCapacity, validStartTime, validEndTime),
                        "name",
                        "Location name cannot exceed 150 characters."
                ),
                Arguments.of(
                        "Capacity less than min",
                        new UpdateLocationRequestDTO(validName, 0, validStartTime, validEndTime),
                        "capacity",
                        "Location capacity must be at least 1."
                ),
                Arguments.of(
                        "Working start time equals end time",
                        new UpdateLocationRequestDTO(validName, validCapacity, sameTime, sameTime),
                        "updateLocationRequestDTO",
                        "Working start time and end time cannot be the same"
                )
        );
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidUpdateRequestProvider")
    @WithMockUser
    void updateLocationById_whenInvalidInput_shouldReturn400BadRequest(
            String testCaseDescription,
            UpdateLocationRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {
        Long locationIdToUpdate = locationDTO1.id();

        ResultActions resultActions = performUpdateLocation(locationIdToUpdate, invalidRequest);

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.messages[0]", is(expectedErrorTarget + ": " + expectedErrorMessage)));

        verify(locationService, never()).updateLocation(anyLong(), any());
    }

    @Test
    @WithMockUser
    void updateLocationById_whenNameIsNullInRequest_shouldUpdateOtherFieldsAndKeepExistingName() throws Exception {
        Long locationIdToUpdate = locationDTO1.id();
        String existingName = locationDTO1.name();

        UpdateLocationRequestDTO requestWithNameNull = new UpdateLocationRequestDTO(
                null,
                updateRequest.capacity(),
                updateRequest.workingStartTime(),
                updateRequest.workingEndTime()
        );

        LocationDTO expectedResponse = new LocationDTO(
                locationIdToUpdate,
                existingName,
                requestWithNameNull.capacity()
        );

        when(locationService.updateLocation(eq(locationIdToUpdate), any(UpdateLocationRequestDTO.class))).thenReturn(expectedResponse);

        ResultActions resultActions = performUpdateLocation(locationIdToUpdate, requestWithNameNull);

        assertLocationResponse(resultActions, expectedResponse);

        ArgumentCaptor<UpdateLocationRequestDTO> dtoCaptor = ArgumentCaptor.forClass(UpdateLocationRequestDTO.class);
        verify(locationService).updateLocation(eq(locationIdToUpdate), dtoCaptor.capture());
        assertNull(dtoCaptor.getValue().name(), "Name in the DTO passed to service should be null.");
    }

    @Test
    @WithMockUser
    void updateLocationById_whenCapacityIsNullInRequest_shouldUpdateOtherFieldsAndKeepExistingCapacity() throws Exception {
        Long locationIdToUpdate = locationDTO1.id();
        Integer existingCapacity = locationDTO1.capacity();

        UpdateLocationRequestDTO requestWithCapacityNull = new UpdateLocationRequestDTO(
                updateRequest.name(),
                null,
                updateRequest.workingStartTime(),
                updateRequest.workingEndTime()
        );

        LocationDTO expectedResponse = new LocationDTO(
                locationIdToUpdate,
                requestWithCapacityNull.name(),
                existingCapacity
        );

        when(locationService.updateLocation(eq(locationIdToUpdate), any(UpdateLocationRequestDTO.class))).thenReturn(expectedResponse);

        ResultActions resultActions = performUpdateLocation(locationIdToUpdate, requestWithCapacityNull);

        assertLocationResponse(resultActions, expectedResponse);

        ArgumentCaptor<UpdateLocationRequestDTO> dtoCaptor = ArgumentCaptor.forClass(UpdateLocationRequestDTO.class);
        verify(locationService).updateLocation(eq(locationIdToUpdate), dtoCaptor.capture());
        assertNull(dtoCaptor.getValue().capacity(), "Capacity in the DTO passed to service should be null.");
    }

    @Test
    @WithMockUser
    void updateLocationById_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = 0L;
        String expectedErrorMessage = "Location not found with id: " + nonExistentLocationId;
        when(locationService.updateLocation(eq(nonExistentLocationId), any(UpdateLocationRequestDTO.class))).thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = performUpdateLocation(nonExistentLocationId, updateRequest);

        assertNotFoundError(resultActions, expectedErrorMessage);

        verify(locationService).updateLocation(eq(nonExistentLocationId), any(UpdateLocationRequestDTO.class));
    }

    @Test
    @WithMockUser
    void updateLocationById_whenServiceThrowsDataIntegrityException_shouldReturn409Conflict() throws Exception {
        Long locationIdToUpdate = locationDTO1.id();
        String malformedJsonRequest = """
                {
                    "name": "Test Room",
                    "capacity": 5,
                    "workingStartTime": "INVALID-TIME-FORMAT",
                    "workingEndTime": "17:00"
                }
                """;

        ResultActions resultActions = mockMvc.perform(put("/api/locations/{id}", locationIdToUpdate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest));

        assertMalformedRequestError(resultActions);

        verify(locationService, never()).createLocation(any());
    }

    @Test
    @WithMockUser
    void updateLocationById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: 'abc'.";

        ResultActions resultActions = mockMvc.perform(put("/api/locations/{id}", invalidId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(objectMapper.writeValueAsString(updateRequest)));

        assertParameterTypeError(resultActions, expectedErrorMessage);
    }

    // === END UPDATE ===

    // === DELETE ===

    @Test
    @WithMockUser
    void deleteLocationById_whenLocationExistsAndNotInUse_shouldReturn204NoContent() throws Exception {
        Long locationIdToDelete = locationDTO1.id();
        doNothing().when(locationService).deleteLocation(locationIdToDelete);

        ResultActions resultActions = mockMvc.perform(delete("/api/locations/{id}", locationIdToDelete));

        resultActions.andExpect(status().isNoContent());

        verify(locationService).deleteLocation(locationIdToDelete);
    }

    @Test
    @WithMockUser
    void deleteLocationById_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = locationDTO1.id();
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        doThrow(new EntityNotFoundException(expectedErrorMessage)).when(locationService).deleteLocation(nonExistentLocationId);

        ResultActions resultActions = mockMvc.perform(delete("/api/locations/{id}", nonExistentLocationId));

        assertNotFoundError(resultActions, expectedErrorMessage);

        verify(locationService).deleteLocation(nonExistentLocationId);
    }

    @Test
    @WithMockUser
    void deleteLocationById_whenLocationInUse_shouldReturn409Conflict() throws Exception {
        Long locationIdToDelete = locationDTO1.id();
        List<Long> conflictingMeetingIds = List.of(locationDTO1.id(), locationDTO2.id());
        String conflictErrorMessage = String.format("Location cannot be deleted because it is used in %d meeting(s). See details.", conflictingMeetingIds.size());
        String conflictIdsMessage = "Conflicting Resource Ids: " +
                conflictingMeetingIds.stream().map(String::valueOf).collect(Collectors.joining(", "));

        doThrow(new ResourceInUseException(conflictErrorMessage, conflictingMeetingIds)).when(locationService).deleteLocation(locationIdToDelete);

        ResultActions resultActions = mockMvc.perform(delete("/api/locations/{id}", locationIdToDelete));

        resultActions
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.CONFLICT.value())))
                .andExpect(jsonPath("$.error", is(HttpStatus.CONFLICT.getReasonPhrase())))
                .andExpect(jsonPath("$.messages[0]", is(conflictErrorMessage)))
                .andExpect(jsonPath("$.messages[1]", is(conflictIdsMessage)));

        verify(locationService).deleteLocation(locationIdToDelete);
    }

    @Test
    @WithMockUser
    void deleteLocationById_whenIdIsInvalidFormat_shouldReturn400BadRequest() throws Exception {
        String invalidId = "abc";
        String expectedErrorMessage = "Parameter 'id' should be of type 'Long' but received value: '" + invalidId + "'.";

        ResultActions resultActions = mockMvc.perform(delete("/api/locations/{id}", invalidId));

        assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(locationService, never()).deleteLocation(anyLong());
    }

    // === END DELETE ===

    // === AVAILABILITY ===

    @Test
    @WithMockUser
    void getLocationAvailability_whenLocationExistsAndValidDate_shouldReturn200OkAndListOfAvailableSlots() throws Exception {
        Long locationId = locationDTO1.id();
        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 14);

        DateTimeFormatter expectedJsonFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<AvailableSlotDTO> expectedSlots = List.of(
                new AvailableSlotDTO(date.atTime(10, 0), date.atTime(11, 0)),
                new AvailableSlotDTO(date.atTime(11, 0), date.atTime(12, 30))
        );

        when(availabilityService.getAvailableTimeForLocation(locationId, date)).thenReturn(expectedSlots);

        ResultActions resultActions = performGetAvailability(locationId, date);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(expectedSlots.size())))
                .andExpect(jsonPath("$[0].startTime", is(expectedSlots.get(0).startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[0].endTime", is(expectedSlots.get(0).endTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].startTime", is(expectedSlots.get(1).startTime().format(expectedJsonFormat))))
                .andExpect(jsonPath("$[1].endTime", is(expectedSlots.get(1).endTime().format(expectedJsonFormat))));

        verify(availabilityService).getAvailableTimeForLocation(locationId, date);
    }

    @Test
    @WithMockUser
    void getLocationAvailability_whenLocationNotFound_shouldReturn404NotFound() throws Exception {
        Long nonExistentLocationId = locationDTO1.id();
        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 14);
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        when(availabilityService.getAvailableTimeForLocation(nonExistentLocationId, date))
                .thenThrow(new EntityNotFoundException(expectedErrorMessage));

        ResultActions resultActions = performGetAvailability(nonExistentLocationId, date);

        assertNotFoundError(resultActions, expectedErrorMessage);

        verify(availabilityService).getAvailableTimeForLocation(nonExistentLocationId, date);
    }

    @Test
    @WithMockUser
    void getLocationAvailability_whenMissingDateParam_shouldReturn400BadRequest() throws Exception {
        Long locationId = locationDTO1.id();
        String expectedErrorMessage = "Required parameter 'date' of type 'LocalDate' is missing.";

        ResultActions resultActions = mockMvc.perform(get("/api/locations/{id}/availability", locationId)
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is("Missing Request Parameter")))
                .andExpect(jsonPath("$.messages[0]", is(expectedErrorMessage)));

        verify(availabilityService, never()).getAvailableTimeForLocation(anyLong(), any(LocalDate.class));
    }

    @Test
    @WithMockUser
    void getLocationAvailability_whenInvalidDateFormat_shouldReturn400BadRequest() throws Exception {
        Long locationId = locationDTO1.id();
        String invalidDateStr = "15-08-2024";
        String expectedErrorMessage = "Parameter 'date' should be of type 'LocalDate' but received value: '15-08-2024'.";

        ResultActions resultActions = mockMvc.perform(get("/api/locations/{id}/availability", locationId)
                .param("date", invalidDateStr)
                .accept(MediaType.APPLICATION_JSON));

        assertParameterTypeError(resultActions, expectedErrorMessage);

        verify(availabilityService, never()).getAvailableTimeForLocation(anyLong(), any(LocalDate.class));
    }

    @Test
    @WithMockUser
    void findLocationAvailabilityByDuration_whenValidRequestDTO_shouldReturn200OkAndResults() throws Exception {
        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 14);
        int durationMinutes = 60;
        Integer minCapacity = 10;
        LocationAvailabilityRequestDTO requestDTO = new LocationAvailabilityRequestDTO(
                date,
                durationMinutes,
                minCapacity
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
        when(availabilityService.getAvailabilityForLocationsByDuration(requestDTO))
                .thenReturn(expectedResults);

        ResultActions resultActions = performAvailabilityByDuration(requestDTO);

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


        verify(availabilityService).getAvailabilityForLocationsByDuration(requestDTO);
    }

    @Test
    @WithMockUser
    void findLocationAvailabilityByDuration_whenMinCapacityIsNull_shouldPassValidationAndCallService() throws Exception {
        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 14);
        int durationMinutes = 60;
        LocationAvailabilityRequestDTO requestDTOWithNullCapacity = new LocationAvailabilityRequestDTO(
                date,
                durationMinutes,
                null
        );

        List<LocationTimeSlotDTO> expectedResults = List.of();
        when(availabilityService.getAvailabilityForLocationsByDuration(requestDTOWithNullCapacity))
                .thenReturn(expectedResults);

        ResultActions resultActions = performAvailabilityByDuration(requestDTOWithNullCapacity);

        resultActions
                .andExpect(status().isOk());

        verify(availabilityService).getAvailabilityForLocationsByDuration(requestDTOWithNullCapacity);
    }

    static Stream<Arguments> invalidLocationAvailabilityRequestProvider() {
        LocalDate validDate = LocalDate.of(Year.now().getValue() + 1, 8, 15);
        int validDuration = 60;
        Integer validMinCapacity = 5;

        LocationAvailabilityRequestDTO missingDateRequest = new LocationAvailabilityRequestDTO(
                null,
                validDuration,
                validMinCapacity
        );

        LocationAvailabilityRequestDTO missingDurationRequest = new LocationAvailabilityRequestDTO(
                validDate,
                null,
                validMinCapacity
        );

        LocationAvailabilityRequestDTO invalidDurationRequest = new LocationAvailabilityRequestDTO(
                validDate,
                0,
                validMinCapacity
        );

        LocationAvailabilityRequestDTO invalidCapacityRequest = new LocationAvailabilityRequestDTO(
                validDate,
                validDuration,
                0
        );

        return Stream.of(
                Arguments.of(
                        "Date is null",
                        missingDateRequest,
                        "date",
                        "A date must be provided."
                ),
                Arguments.of(
                        "Duration is null",
                        missingDurationRequest,
                        "durationMinutes",
                        "Minimum duration must be provided."
                ),
                Arguments.of(
                        "Duration less than min",
                        invalidDurationRequest,
                        "durationMinutes",
                        "Duration must be at least 1 minute."
                ),
                Arguments.of(
                        "Minimum capacity less than min (when provided)",
                        invalidCapacityRequest,
                        "minimumCapacity",
                        "Minimum capacity must be at least 1 if provided."
                )
        );
    }

    @ParameterizedTest(name = "Validation Error: {0}")
    @MethodSource("invalidLocationAvailabilityRequestProvider")
    @WithMockUser
    void findLocationAvailabilityByDuration_whenInvalidRequestDTO_shouldReturn400BadRequest(
            String testCaseDescription,
            LocationAvailabilityRequestDTO invalidRequest,
            String expectedErrorTarget,
            String expectedErrorMessage
    ) throws Exception {

        ResultActions resultActions = performAvailabilityByDuration(invalidRequest);

        assertValidationError(resultActions, expectedErrorTarget, expectedErrorMessage);

        verify(availabilityService, never()).getAvailabilityForLocationsByDuration(any(LocationAvailabilityRequestDTO.class));
    }

    @Test
    @WithMockUser
    void findLocationAvailabilityByDuration_whenInvalidDateFormatInRequest_shouldReturn400BadRequest() throws Exception {
        String malformedJsonRequest = """
                {
                    "date": "15-08-2024",  // Invalid format (DD-MM-YYYY instead of YYYY-MM-DD)
                    "durationMinutes": 60,
                    "minimumCapacity": 5
                }
                """;

        ResultActions resultActions = mockMvc.perform(post("/api/locations/availability-by-duration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJsonRequest)
                .accept(MediaType.APPLICATION_JSON));

        assertMalformedRequestError(resultActions);

        verify(availabilityService, never()).getAvailabilityForLocationsByDuration(any());
    }

    // === END AVAILABILITY ===

    // === HELPER METHODS ===

    // --- BUILD REQUEST ---

    // POST requests
    private ResultActions performCreateLocation(CreateLocationRequestDTO request) throws Exception {
        return mockMvc.perform(post("/api/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // PUT requests
    private ResultActions performUpdateLocation(Long id, UpdateLocationRequestDTO request) throws Exception {
        return mockMvc.perform(put("/api/locations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // GET All requests
    private ResultActions performGetAllLocations() throws Exception {
        return mockMvc.perform(get("/api/locations")
                .accept(MediaType.APPLICATION_JSON));
    }

    // GET by ID requests
    private ResultActions performGetLocation(Long id) throws Exception {
        return mockMvc.perform(get("/api/locations/{id}", id)
                .accept(MediaType.APPLICATION_JSON));
    }

    // GET Availability by ID requests
    private ResultActions performGetAvailability(Long id, LocalDate date) throws Exception {
        return mockMvc.perform(get("/api/locations/{id}/availability", id)
                .param("date", date.toString())
                .accept(MediaType.APPLICATION_JSON));
    }

    // Availability by duration requests
    private ResultActions performAvailabilityByDuration(LocationAvailabilityRequestDTO request) throws Exception {
        return mockMvc.perform(post("/api/locations/availability-by-duration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON));
    }

    // --- END BUILD REQUEST ---

    // --- ASSERT SUCCESSFUL RESPONSE ---

    // For successful location responses (200)
    private void assertLocationResponse(ResultActions resultActions, LocationDTO expected) throws Exception {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(expected.id().intValue())))
                .andExpect(jsonPath("$.name", is(expected.name())))
                .andExpect(jsonPath("$.capacity", is(expected.capacity())));
    }

    // For created responses (201)
    private void assertCreatedLocationResponse(ResultActions resultActions, LocationDTO expected) throws Exception {
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(expected.id().intValue())))
                .andExpect(jsonPath("$.name", is(expected.name())))
                .andExpect(jsonPath("$.capacity", is(expected.capacity())));
    }

    // For location lists (200)
    private void assertLocationListResponse(ResultActions resultActions, List<LocationDTO> expected) throws Exception {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(expected.size())));

        for (int i = 0; i < expected.size(); i++) {
            LocationDTO location = expected.get(i);
            resultActions
                    .andExpect(jsonPath("$[" + i + "].id", is(location.id().intValue())))
                    .andExpect(jsonPath("$[" + i + "].name", is(location.name())))
                    .andExpect(jsonPath("$[" + i + "].capacity", is(location.capacity())));
        }
    }

    // --- END ASSERT SUCCESSFUL RESPONSE ---

    // --- ASSERT ERROR RESPONSE ---

    // Generic error response checker
    private void assertErrorResponse(ResultActions resultActions, HttpStatus expectedStatus,
                                     String expectedError, String expectedMessage) throws Exception {
        resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(expectedStatus.value())))
                .andExpect(jsonPath("$.error", is(expectedError)))
                .andExpect(jsonPath("$.messages[0]", is(expectedMessage)));
    }

    // Validation error (400)
    private void assertValidationError(ResultActions resultActions, String expectedTarget,
                                       String expectedMessage) throws Exception {
        assertErrorResponse(resultActions, HttpStatus.BAD_REQUEST, "Validation Failed",
                expectedTarget + ": " + expectedMessage);
        resultActions.andExpect(jsonPath("$.messages.length()", is(1)));
    }

    // Not found errors (404)
    private void assertNotFoundError(ResultActions resultActions, String expectedMessage) throws Exception {
        assertErrorResponse(resultActions, HttpStatus.NOT_FOUND, "Resource Not Found", expectedMessage);
    }

    // Conflict errors (409)
    private void assertConflictError(ResultActions resultActions, String expectedMessage) throws Exception {
        assertErrorResponse(resultActions, HttpStatus.CONFLICT, "Data Conflict/Integrity Violation", expectedMessage);
    }

    // Parameter type errors
    private void assertParameterTypeError(ResultActions resultActions, String expectedMessage) throws Exception {
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is("Invalid Parameter Type/Format")))
                .andExpect(jsonPath("$.messages[0]", containsString(expectedMessage)));
    }

    // Malformed request body errors
    private void assertMalformedRequestError(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", containsString("Malformed Request Body")))
                .andExpect(jsonPath("$.messages[0]", containsString("Request body is malformed or contains invalid data/format.")));
    }

    // --- END ASSERT ERROR RESPONSE ---

    // === END HELPER METHODS
}
