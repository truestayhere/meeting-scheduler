package com.truestayhere.meeting_scheduler.service;


import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enable Mockito
public class LocationServiceTest {

    private final Long DEFAULT_LOCATION_ID = 1L;
    private final String DEFAULT_LOCATION_NAME = "Room 1";
    private final Integer DEFAULT_LOCATION_CAPACITY = 10;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private LocationMapper locationMapper;
    @InjectMocks
    private LocationService locationService;
    private CreateLocationRequestDTO defaultCreateRequest;
    private UpdateLocationRequestDTO defaultUpdateRequest;
    private Location defaultLocation;
    private Location defaultSavedLocation;
    private LocationDTO defaultLocationDTO;

    @BeforeEach
    void setUp() {

        // Create location request
        defaultCreateRequest = new CreateLocationRequestDTO(
                DEFAULT_LOCATION_NAME,
                DEFAULT_LOCATION_CAPACITY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        // Update location request (all fields updated by default)
        defaultUpdateRequest = new UpdateLocationRequestDTO(
                "Updated " + DEFAULT_LOCATION_NAME,
                DEFAULT_LOCATION_CAPACITY + 10,
                LocalTime.of(6, 0),
                LocalTime.of(16, 0)
        );

        // Simulate a new location object
        defaultLocation = new Location();
        defaultLocation.setId(DEFAULT_LOCATION_ID);
        defaultLocation.setName(DEFAULT_LOCATION_NAME);
        defaultLocation.setCapacity(DEFAULT_LOCATION_CAPACITY);
        defaultLocation.setWorkingStartTime(LocalTime.of(9, 0));
        defaultLocation.setWorkingEndTime(LocalTime.of(17, 0));

        // Simulate an object that the repo returns after saving
        defaultSavedLocation = new Location();
        defaultSavedLocation.setId(DEFAULT_LOCATION_ID);
        defaultSavedLocation.setName(DEFAULT_LOCATION_NAME);
        defaultSavedLocation.setCapacity(DEFAULT_LOCATION_CAPACITY);
        defaultSavedLocation.setWorkingStartTime(LocalTime.of(9, 0));
        defaultSavedLocation.setWorkingEndTime(LocalTime.of(17, 0));

        // Simulate response DTO
        defaultLocationDTO = new LocationDTO(
                DEFAULT_LOCATION_ID,
                DEFAULT_LOCATION_NAME,
                DEFAULT_LOCATION_CAPACITY
        );
    }


    @Test
    void createLocation_shouldReturnLocationDTO_whenSuccessful() {
        // Arrange (Given) - Define the behavior of the mocks
        when(locationRepository.findByName(defaultCreateRequest.name())).thenReturn(Optional.empty());
        when(locationMapper.mapToLocation(defaultCreateRequest)).thenReturn(defaultLocation);
        when(locationRepository.save(defaultLocation)).thenReturn(defaultSavedLocation);
        when(locationMapper.mapToLocationDTO(defaultSavedLocation)).thenReturn(defaultLocationDTO);

        // Act (When) - Call the method for testing
        LocationDTO result = locationService.createLocation(defaultCreateRequest);

        // Assert (Then) - Verify the outcome
        assertNotNull(result);
        assertEquals(DEFAULT_LOCATION_NAME, result.name());
        assertEquals(DEFAULT_LOCATION_CAPACITY, result.capacity());
        assertEquals(DEFAULT_LOCATION_ID, result.id());

        // Verify that mock methods were called as expected
        verify(locationRepository).findByName(defaultCreateRequest.name());
        verify(locationMapper).mapToLocation(defaultCreateRequest);
        verify(locationRepository).save(defaultLocation);
        verify(locationMapper).mapToLocationDTO(defaultSavedLocation);
    }

    @Test
    void createLocation_shouldThrowIllegalArgumentException_whenNameIsDuplicate() {
        // Arrange
        String duplicateName = DEFAULT_LOCATION_NAME;
        String expectedErrorMessage = "Location with name '" + duplicateName + "' already exists.";
        when(locationRepository.findByName(duplicateName)).thenReturn(Optional.of(defaultLocation));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            locationService.createLocation(defaultCreateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        // - Verify
        verify(locationRepository).findByName(duplicateName);
        verify(locationMapper, never()).mapToLocation(defaultCreateRequest);
        verify(locationRepository, never()).save(any(Location.class));
        verify(locationMapper, never()).mapToLocationDTO(defaultSavedLocation);
    }


    @Test
    void getAllLocations_shouldReturnListOfLocationDTOs_whenLocationsExist() {
        // Set up location entities
        Location location1 = new Location();

        location1.setId(DEFAULT_LOCATION_ID);
        location1.setName(DEFAULT_LOCATION_NAME);
        location1.setCapacity(DEFAULT_LOCATION_CAPACITY);
        location1.setWorkingStartTime(LocalTime.of(8, 0));
        location1.setWorkingEndTime(LocalTime.of(18, 0));

        Location location2 = new Location();

        location2.setId(DEFAULT_LOCATION_ID + 1);
        location2.setName(DEFAULT_LOCATION_NAME + " (2)");
        location2.setCapacity(DEFAULT_LOCATION_CAPACITY + 10);
        location2.setWorkingStartTime(LocalTime.of(9, 0));
        location2.setWorkingEndTime(LocalTime.of(17, 0));

        List<Location> mockLocations = List.of(location1, location2);

        // Set up locations DTOs
        LocationDTO dto1 = new LocationDTO(DEFAULT_LOCATION_ID, DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_CAPACITY);
        LocationDTO dto2 = new LocationDTO((DEFAULT_LOCATION_ID + 1), (DEFAULT_LOCATION_NAME + " (2)"), (DEFAULT_LOCATION_CAPACITY + 10));

        List<LocationDTO> mockLocationDTOs = List.of(dto1, dto2);

        when(locationRepository.findAll()).thenReturn(mockLocations);
        when(locationMapper.mapToLocationDTOList(mockLocations)).thenReturn(mockLocationDTOs);

        List<LocationDTO> results = locationService.getAllLocations();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsAll(mockLocationDTOs)
                && mockLocationDTOs.containsAll(results));

        verify(locationRepository).findAll();
        verify(locationMapper).mapToLocationDTOList(mockLocations);
        verify(locationMapper, never()).mapToLocationDTO(any(Location.class));
    }


    @Test
    void getAllLocations_shouldReturnEmptyList_whenNoLocationsExist() {
        List<Location> emptyLocationsList = List.of();
        List<LocationDTO> emptyDTOList = List.of();

        when(locationRepository.findAll()).thenReturn(emptyLocationsList);
        when(locationMapper.mapToLocationDTOList(emptyLocationsList)).thenReturn(emptyDTOList);

        List<LocationDTO> results = locationService.getAllLocations();

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(locationRepository).findAll();
        verify(locationMapper).mapToLocationDTOList(emptyLocationsList);
    }


    @Test
    void getLocationById_shouldReturnLocationDTO_whenLocationExists() {
        // Arrange
        Long locationId = DEFAULT_LOCATION_ID;
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(defaultLocation));
        when(locationMapper.mapToLocationDTO(defaultLocation)).thenReturn(defaultLocationDTO);

        // Act
        LocationDTO result = locationService.getLocationById(locationId);

        // Assert
        assertNotNull(result);
        assertEquals(locationId, result.id());
        assertEquals(DEFAULT_LOCATION_NAME, result.name());

        // Verify
        verify(locationRepository).findById(locationId);
        verify(locationMapper).mapToLocationDTO(defaultLocation);
    }


    @Test
    void getLocationById_shouldThrowEntityNotFoundException_whenLocationDoesNotExist() {
        Long nonExistentLocationId = DEFAULT_LOCATION_ID;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        // Repo returns empty object
        when(locationRepository.findById(nonExistentLocationId)).thenReturn(Optional.empty());

        // Ensure that the exception was thrown on the code execution
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> locationService.getLocationById(nonExistentLocationId)
        );

        // Check that the error message is correct
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(nonExistentLocationId);
        // Verify that mapper is never called on the code execution
        verify(locationMapper, never()).mapToLocationDTO(any(Location.class));
    }


    @Test
    void updateLocation_shouldReturnUpdatedLocationDTO_whenSomeFieldsUpdated() {
        Long locationId = DEFAULT_LOCATION_ID;

        UpdateLocationRequestDTO updateRequest = new UpdateLocationRequestDTO(
                "Updated Room",
                null,
                null,
                LocalTime.of(20, 0)
        );

        Location existingLocation = new Location();
        existingLocation.setId(locationId);
        existingLocation.setName("Original Room");
        existingLocation.setCapacity(25);
        existingLocation.setWorkingStartTime(LocalTime.of(8, 0));
        existingLocation.setWorkingEndTime(LocalTime.of(18, 0));

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findByName(updateRequest.name())).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            UpdateLocationRequestDTO dtoArg = invocation.getArgument(0);
            Location entityArg = invocation.getArgument(1);
            if (dtoArg.name() != null) entityArg.setName(dtoArg.name());
            if (dtoArg.capacity() != null) entityArg.setCapacity(dtoArg.capacity());
            if (dtoArg.workingStartTime() != null) entityArg.setWorkingStartTime(dtoArg.workingStartTime());
            if (dtoArg.workingEndTime() != null) entityArg.setWorkingEndTime(dtoArg.workingEndTime());
            return null;
        }).when(locationMapper).updateLocationFromDto(eq(updateRequest), any(Location.class));

        LocationDTO expectedResponse = new LocationDTO(
                locationId,
                "Updated Room",
                25
        );
        when(locationMapper.mapToLocationDTO(any(Location.class))).thenReturn(expectedResponse);

        LocationDTO result = locationService.updateLocation(locationId, updateRequest);

        ArgumentCaptor<Location> entityCaptor = ArgumentCaptor.forClass(Location.class);
        verify(locationMapper).mapToLocationDTO(entityCaptor.capture());
        Location capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity);
        assertEquals("Updated Room", capturedEntity.getName());
        assertEquals(25, capturedEntity.getCapacity());
        assertEquals(LocalTime.of(8, 0), capturedEntity.getWorkingStartTime());
        assertEquals(LocalTime.of(20, 0), capturedEntity.getWorkingEndTime());

        assertEquals(expectedResponse, result);

        verify(locationRepository).findById(locationId);
        verify(locationRepository).findByName("Updated Room");
    }

    @Test
    void updateLocation_shouldReturnUpdatedLocationDTO_whenAllFieldsUpdated() {
        Long locationId = DEFAULT_LOCATION_ID;

        UpdateLocationRequestDTO updateRequest = new UpdateLocationRequestDTO(
                "Fully Updated Room",
                50,
                LocalTime.of(7, 0),
                LocalTime.of(19, 0)
        );

        Location existingLocation = new Location();
        existingLocation.setId(locationId);
        existingLocation.setName("Original Room");
        existingLocation.setCapacity(10);
        existingLocation.setWorkingStartTime(LocalTime.of(9, 0));
        existingLocation.setWorkingEndTime(LocalTime.of(17, 0));

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findByName(updateRequest.name())).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            UpdateLocationRequestDTO dtoArg = invocation.getArgument(0);
            Location entityArg = invocation.getArgument(1);
            if (dtoArg.name() != null) entityArg.setName(dtoArg.name());
            if (dtoArg.capacity() != null) entityArg.setCapacity(dtoArg.capacity());
            if (dtoArg.workingStartTime() != null) entityArg.setWorkingStartTime(dtoArg.workingStartTime());
            if (dtoArg.workingEndTime() != null) entityArg.setWorkingEndTime(dtoArg.workingEndTime());
            return null;
        }).when(locationMapper).updateLocationFromDto(eq(updateRequest), any(Location.class));

        LocationDTO expectedResponse = new LocationDTO(
                locationId,
                updateRequest.name(),
                updateRequest.capacity()
        );

        when(locationMapper.mapToLocationDTO(any(Location.class))).thenReturn(expectedResponse);

        LocationDTO result = locationService.updateLocation(locationId, updateRequest);

        ArgumentCaptor<Location> entityCaptor = ArgumentCaptor.forClass(Location.class);
        verify(locationMapper).mapToLocationDTO(entityCaptor.capture());
        Location capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity);
        assertEquals("Fully Updated Room", capturedEntity.getName());
        assertEquals(50, capturedEntity.getCapacity());
        assertEquals(LocalTime.of(7, 0), capturedEntity.getWorkingStartTime());
        assertEquals(LocalTime.of(19, 0), capturedEntity.getWorkingEndTime());

        assertEquals(expectedResponse, result);

        verify(locationRepository).findById(locationId);
        verify(locationRepository).findByName(updateRequest.name());
    }

    @Test
    void updateLocation_shouldSucceed_whenDuplicatesCheckReturnsOnlyTheLocationBeingUpdated() {
        Long locationId = DEFAULT_LOCATION_ID;
        String originalName = "Original Name";

        UpdateLocationRequestDTO updateRequest = new UpdateLocationRequestDTO(
                null,
                15,
                null,
                null
        );

        Location existingLocation = new Location();
        existingLocation.setId(locationId);
        existingLocation.setName(originalName);
        existingLocation.setCapacity(10);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findByName(originalName)).thenReturn(Optional.of(existingLocation));
        doAnswer(invocation -> {
            UpdateLocationRequestDTO dtoArg = invocation.getArgument(0);
            Location entityArg = invocation.getArgument(1);
            if (dtoArg.capacity() != null) entityArg.setCapacity(dtoArg.capacity());
            return null;
        }).when(locationMapper).updateLocationFromDto(eq(updateRequest), any(Location.class));

        LocationDTO expectedResponse = new LocationDTO(
                locationId,
                originalName,
                15
        );
        when(locationMapper.mapToLocationDTO(any(Location.class))).thenReturn(expectedResponse);

        assertDoesNotThrow(() -> {
            locationService.updateLocation(locationId, updateRequest);
        }, "Update should succeed when duplicate check finds the same location.");

        verify(locationRepository).findById(locationId);
        verify(locationRepository).findByName(originalName);
        verify(locationMapper).updateLocationFromDto(eq(updateRequest), any(Location.class));
        verify(locationMapper).mapToLocationDTO(any(Location.class));
    }

    @Test
    void updateLocation_shouldThrowEntityNotFoundException_whenLocationDoesNotExist() {
        // Arrange
        Long nonExistentLocationId = DEFAULT_LOCATION_ID;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        when(locationRepository.findById(nonExistentLocationId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            locationService.updateLocation(nonExistentLocationId, defaultUpdateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        // - Verify
        verify(locationRepository).findById(nonExistentLocationId);
        verify(locationRepository, never()).findByName(anyString());
        verify(locationMapper, never()).updateLocationFromDto(any(), any());
        verify(locationRepository, never()).save(any(Location.class));
        verify(locationMapper, never()).mapToLocationDTO(any(Location.class));
    }


    @Test
    void updateLocation_shouldThrowIllegalArgumentException_whenNameConflictsWithAnotherLocation() {
        // Assign
        Long locationIdBeingUpdated = DEFAULT_LOCATION_ID + 1;
        Long otherLocationIdWithSameName = defaultLocation.getId();
        String conflictingName = defaultLocation.getName();

        UpdateLocationRequestDTO updateRequestWithConflictingName = new UpdateLocationRequestDTO(
                conflictingName, defaultUpdateRequest.capacity(), defaultUpdateRequest.workingStartTime(), defaultUpdateRequest.workingEndTime()
        );

        Location locationBeingUpdated = new Location();
        locationBeingUpdated.setId(locationIdBeingUpdated);
        locationBeingUpdated.setName("Old name");

        String expectedErrorMessage = "Another location (" +
                otherLocationIdWithSameName + ") already exists with name '"
                + conflictingName + "'.";

        when(locationRepository.findById(locationIdBeingUpdated)).thenReturn(Optional.of(locationBeingUpdated));
        when(locationRepository.findByName(conflictingName)).thenReturn(Optional.of(defaultLocation));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            locationService.updateLocation(locationIdBeingUpdated, updateRequestWithConflictingName);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        // - Verify
        verify(locationRepository).findById(locationIdBeingUpdated);
        verify(locationRepository).findByName(conflictingName);
        verify(locationMapper, never()).updateLocationFromDto(any(), any());
        verify(locationRepository, never()).save(any(Location.class));
        verify(locationMapper, never()).mapToLocationDTO(any(Location.class));
    }


    @Test
    void deleteLocation_shouldDeleteLocation_whenLocationExistsAndNotInUse() {
        // Arrange
        Long locationIdToDelete = DEFAULT_LOCATION_ID;

        when(locationRepository.existsById(locationIdToDelete)).thenReturn(true);
        when(meetingRepository.findByLocation_id(locationIdToDelete)).thenReturn(List.of());
        doNothing().when(locationRepository).deleteById(locationIdToDelete);

        // Act
        assertDoesNotThrow(() -> locationService.deleteLocation(locationIdToDelete));

        // Assert
        verify(locationRepository).existsById(locationIdToDelete);
        verify(meetingRepository).findByLocation_id(locationIdToDelete);
        verify(locationRepository).deleteById(locationIdToDelete);
    }


    @Test
    void deleteLocation_shouldThrowEntityNotFoundException_whenLocationDoesNotExist() {
        // Arrange
        Long nonExistentLocationId = DEFAULT_LOCATION_ID;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        when(locationRepository.existsById(nonExistentLocationId)).thenReturn(false);

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            locationService.deleteLocation(nonExistentLocationId);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        // - Verify
        verify(locationRepository).existsById(nonExistentLocationId);
        verify(meetingRepository, never()).findByLocation_id(anyLong());
        verify(locationRepository, never()).deleteById(anyLong());
    }


    @Test
    void deleteLocation_shouldThrowResourceInUseException_whenLocationIsInUseByMeetings() {
        Long locationIdToDelete = DEFAULT_LOCATION_ID;

        Meeting meeting1 = new Meeting();
        meeting1.setId(101L);
        Meeting meeting2 = new Meeting();
        meeting2.setId(102L);
        List<Meeting> conflictingMeetingsList = Arrays.asList(meeting1, meeting2);
        List<Long> expectedMeetingIds = Arrays.asList(101L, 102L);

        when(locationRepository.existsById(locationIdToDelete)).thenReturn(true);
        when(meetingRepository.findByLocation_id(locationIdToDelete)).thenReturn(conflictingMeetingsList);

        String expectedErrorMessage = String.format(
                "Location cannot be deleted because it is used in %d meeting(s). See details.",
                expectedMeetingIds.size()
        );

        ResourceInUseException exception = assertThrows(ResourceInUseException.class, () -> {
            locationService.deleteLocation(locationIdToDelete);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());
        assertNotNull(exception.getConflictingResourceIds(), "Conflicting IDs should be present in the exception.");
        assertTrue(exception.getConflictingResourceIds().containsAll(expectedMeetingIds) && expectedMeetingIds.containsAll(exception.getConflictingResourceIds()), "Conflicting IDs in exception do not match expected IDs.");
        assertEquals(expectedMeetingIds.size(), exception.getConflictingResourceIds().size(), "Number of conflicting IDs doesn't match");

        verify(locationRepository).existsById(locationIdToDelete);
        verify(meetingRepository).findByLocation_id(locationIdToDelete);
        verify(locationRepository, never()).deleteById(anyLong());
    }

}
