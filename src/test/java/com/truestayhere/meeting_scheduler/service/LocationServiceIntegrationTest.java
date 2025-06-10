package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.AbstractIntegrationTest;
import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LocationService locationService;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean the database to ensure tests are isolated
        meetingRepository.deleteAll();
        locationRepository.deleteAll();
        attendeeRepository.deleteAll();
    }

    // Create Operations

    @Test
    void shouldCreateAndFindLocationSuccessfully() {
        CreateLocationRequestDTO requestDTO = new CreateLocationRequestDTO(
                "Room 1",
                10,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        LocationDTO createdLocation = locationService.createLocation(requestDTO);
        assertThat(createdLocation.id()).isNotNull();
        assertThat(createdLocation.name()).isEqualTo("Room 1");

        LocationDTO foundLocation = locationService.getLocationById(createdLocation.id());
        assertThat(foundLocation).isNotNull();
        assertThat(foundLocation.name()).isEqualTo("Room 1");
        assertThat(foundLocation.capacity()).isEqualTo(10);
    }

    @Test
    void shouldThrowExceptionWhenCreatingLocationWithDuplicateName() {
        String duplicateName = "Duplicate name";

        Location location = new Location(duplicateName, 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location savedLocation = locationRepository.save(location);

        CreateLocationRequestDTO requestDTOWithDuplicateName = new CreateLocationRequestDTO(
                duplicateName,
                10,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        String expectedErrorMessage = "Location with name '" + duplicateName + "' already exists.";
        IllegalArgumentException thrownException = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    locationService.createLocation(requestDTOWithDuplicateName);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Read Operations

    @Test
    void shouldFindAllLocations() {
        Location location1 = new Location("Room 1", 10);
        location1.setWorkingStartTime(LocalTime.of(8, 0));
        location1.setWorkingEndTime(LocalTime.of(18, 0));
        Location location2 = new Location("Room 2", 5);
        location1.setWorkingStartTime(LocalTime.of(9, 0));
        location1.setWorkingEndTime(LocalTime.of(17, 0));
        locationRepository.saveAll(List.of(location1, location2));

        List<LocationDTO> foundLocations = locationService.getAllLocations();
        assertThat(foundLocations).isNotNull();
        assertThat(foundLocations).hasSize(2);
        assertThat(foundLocations)
                .extracting(LocationDTO::name)
                .containsExactlyInAnyOrder("Room 1", "Room 2");
        assertThat(foundLocations)
                .extracting(LocationDTO::capacity)
                .containsExactlyInAnyOrder(10, 5);
    }

    @Test
    void shouldReturnEmptyListWhenNoLocationsExist() {
        List<LocationDTO> locationsFromRepo = locationService.getAllLocations();
        assertThat(locationsFromRepo).isNotNull().isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenFindingNonExistentLocation() {
        Long nonExistentLocationId = 0L;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    locationService.getLocationById(nonExistentLocationId);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Update Operations

    @Test
    void shouldUpdateLocationSuccessfully() {
        Location location = new Location("Room 1", 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location savedLocation = locationRepository.save(location);
        Long locationId = savedLocation.getId();

        UpdateLocationRequestDTO requestDTO = new UpdateLocationRequestDTO(
                "Room 1 Updated",
                20,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        LocationDTO updatedLocationDTO = locationService.updateLocation(locationId, requestDTO);
        assertThat(updatedLocationDTO.name()).isEqualTo("Room 1 Updated");
        assertThat(updatedLocationDTO.capacity()).isEqualTo(20);

        Location updatedLocationFromDb = locationRepository.findById(locationId)
                .orElseThrow(() -> new AssertionError("Location not found after update"));
        assertThat(updatedLocationFromDb.getName()).isEqualTo("Room 1 Updated");
        assertThat(updatedLocationFromDb.getCapacity()).isEqualTo(20);
        assertThat(updatedLocationFromDb.getWorkingStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(updatedLocationFromDb.getWorkingEndTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentLocation() {
        Long nonExistentLocationId = 0L;
        UpdateLocationRequestDTO requestDTO = new UpdateLocationRequestDTO(null, null, null, null);
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    locationService.updateLocation(nonExistentLocationId, requestDTO);
                },
                "Expected getLocationById to throw, but it didn't"
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingLocationWithDuplicateName() {
        String duplicateName = "Duplicate name";

        Location location = new Location("Room 1", 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location locationToUpdate = locationRepository.save(location);
        Long locationIdToUpdate  = locationToUpdate.getId();

        Location conflictingLocation = new Location(duplicateName, 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location savedConflictingLocation = locationRepository.save(conflictingLocation);
        Long conflictingLocationId = savedConflictingLocation.getId();

        UpdateLocationRequestDTO requestDTOWithDuplicateName = new UpdateLocationRequestDTO(
                duplicateName,
                null,
                null,
                null
        );

        String expectedErrorMessage = "Another location (" + conflictingLocationId + ") already exists with name '" + duplicateName + "'.";
        IllegalArgumentException thrownException = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    locationService.updateLocation(locationIdToUpdate, requestDTOWithDuplicateName);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Delete Operations

    @Test
    void shouldDeleteLocationSuccessfully() {
        Location location = new Location("Room 1", 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location savedLocation = locationRepository.save(location);
        Long locationId = savedLocation.getId();

        assertThat(locationRepository.findById(locationId)).isPresent();

        locationService.deleteLocation(locationId);

        Optional<Location> locationOptional = locationRepository.findById(locationId);
        assertThat(locationOptional).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentLocation() {
        Long nonExistentLocationId = 0L;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    locationService.deleteLocation(nonExistentLocationId);
                },
                "Expected getLocationById to throw, but it didn't"
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldThrowResourceInUseExceptionWhenDeletingLocationWithMeetings() {
        Attendee attendee = new Attendee(
                "Attendee Name",
                "attendeename@test.com",
                passwordEncoder.encode("password"),
                "ROLE_USER"
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        attendeeRepository.save(attendee);

        Location location = new Location("Room 1", 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location locationInUse = locationRepository.save(location);
        Long locationIdToDelete = locationInUse.getId();

        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 15);
        Meeting meeting = new Meeting(
                "Title",
                date.atTime(11, 0),
                date.atTime(12, 0),
                location
        );
        meeting.addAttendee(attendee);
        meetingRepository.save(meeting);

        String expectedErrorMessage = String.format("Location cannot be deleted because it is used in %d meeting(s). See details.", 1);
        ResourceInUseException thrownException = assertThrows(
                ResourceInUseException.class,
                () -> {
                    locationService.deleteLocation(locationIdToDelete);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
        assertThat(locationRepository.findById(locationIdToDelete)).isPresent();
    }

    // Concurrent updates testing

    // Optimistic locking test (Repository Level)
    // Note: Service-level optimistic locking tests aren't needed because
    // @Transactional methods always fetch fresh data
    @Test
    void shouldThrowOptimisticLockingException_whenUpdatingStaleMeeting() {

        Location initialLocation = new Location(
                "Initial Location",
                10
        );
        initialLocation.setWorkingStartTime(LocalTime.of(9, 0));
        initialLocation.setWorkingEndTime(LocalTime.of(17, 0));
        Location savedInitialLocation = locationRepository.save(initialLocation);
        Long locationId = savedInitialLocation.getId();

        // User 1 and User 2 fetch the location at the same time
        Location locationUser1 = locationRepository.findById(locationId).orElseThrow();
        Location locationUser2 = locationRepository.findById(locationId).orElseThrow();

        // Version must be 0 for both users
        assertThat(locationUser1.getVersion()).isEqualTo(0);
        assertThat(locationUser2.getVersion()).isEqualTo(0);

        // First user updates the location
        locationUser1.setName("Updated by User 1");
        locationRepository.saveAndFlush(locationUser1);

        // The location version must be 1 now
        Location updatedLocation = locationRepository.findById(locationId).orElseThrow();
        assertThat(updatedLocation.getVersion()).isEqualTo(1);
        assertThat(updatedLocation.getName()).isEqualTo("Updated by User 1");

        // User 2 attempts to update the location
        locationUser2.setName("Updated by User 2");

        // The update should fail for User 2
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            locationRepository.saveAndFlush(locationUser2);
        });
    }

    @Test
    void shouldPreserveBothUpdates_whenUsersUpdateDifferentFields() {
        CreateLocationRequestDTO createRequest = new CreateLocationRequestDTO(
                "Location Name",
                3,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );
        LocationDTO initialLocation = locationService.createLocation(createRequest);
        Long locationId = initialLocation.id();

        // Update 1
        UpdateLocationRequestDTO titleUpdate = new UpdateLocationRequestDTO(
                "Location Name Updated", null, null, null
        );
        locationService.updateLocation(locationId, titleUpdate);

        // Update 2
        UpdateLocationRequestDTO capacityUpdate = new UpdateLocationRequestDTO(
                null, 20, null, null
        );
        locationService.updateLocation(locationId, capacityUpdate);

        // BOTH changes should succeed
        LocationDTO finalLocation = locationService.getLocationById(locationId);
        assertThat(finalLocation.name()).isEqualTo("Location Name Updated");
        assertThat(finalLocation.capacity()).isEqualTo(20);
    }

    @Test
    void shouldHandleRapidMultipleUpdates_withoutDataLoss() {
        CreateLocationRequestDTO createRequest = new CreateLocationRequestDTO(
                "Location Name",
                3,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );
        LocationDTO initialLocation = locationService.createLocation(createRequest);
        Long locationId = initialLocation.id();

        List<String> names = List.of("Update 1", "Update 2", "Update 3", "Final Update");

        for (String name : names) {
            UpdateLocationRequestDTO nameUpdate = new UpdateLocationRequestDTO(
                    name, null, null, null
            );
            assertDoesNotThrow(() -> locationService.updateLocation(locationId, nameUpdate));
        }

        // The final state should reflect the last update
        LocationDTO finalLocation = locationService.getLocationById(locationId);
        assertThat(finalLocation.name()).isEqualTo("Final Update");
    }
}
