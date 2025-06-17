package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.AbstractIntegrationTest;
import com.truestayhere.meeting_scheduler.dto.request.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AttendeeDTO;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.model.Role;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AttendeeServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AttendeeService attendeeService;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        locationRepository.deleteAll();
        attendeeRepository.deleteAll();
    }

    // Create Operations

    @Test
    void shouldCreateAndFindAttendeeSuccessfully() {
        CreateAttendeeRequestDTO requestDTO = new CreateAttendeeRequestDTO(
                "Attendee Name",
                "attendeename@test.com",
                "rawPassword",
                Role.USER,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        AttendeeDTO createdAttendee = attendeeService.createAttendee(requestDTO);
        assertThat(createdAttendee.id()).isNotNull();
        assertThat(createdAttendee.email()).isEqualTo("attendeename@test.com");

        AttendeeDTO foundAttendee = attendeeService.getAttendeeById(createdAttendee.id());
        assertThat(foundAttendee).isNotNull();
        assertThat(foundAttendee.name()).isEqualTo("Attendee Name");
        assertThat(foundAttendee.email()).isEqualTo("attendeename@test.com");
    }

    @Test
    void shouldThrowExceptionWhenCreatingAttendeeWithDuplicateEmail() {
        String duplicateEmail = "duplicate@test.com";

        Attendee attendee = new Attendee(
                "Attendee Name",
                duplicateEmail,
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        attendeeRepository.save(attendee);

        CreateAttendeeRequestDTO requestDTOWithDuplicateEmail = new CreateAttendeeRequestDTO(
                "Attendee Name",
                duplicateEmail,
                "rawPassword",
                Role.USER,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        String expectedErrorMessage = "Attendee with email '" + duplicateEmail + "' already exists.";
        IllegalArgumentException thrownException = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    attendeeService.createAttendee(requestDTOWithDuplicateEmail);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Read Operations

    @Test
    void shouldFindAllAttendees() {
        Attendee attendee1 = new Attendee(
                "Attendee One",
                "attendeeone@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee1.setWorkingStartTime(LocalTime.of(7, 30));
        attendee1.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee attendee2 = new Attendee(
                "Attendee Two",
                "attendeetwo@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee2.setWorkingStartTime(LocalTime.of(7, 30));
        attendee2.setWorkingEndTime(LocalTime.of(16, 30));
        attendeeRepository.saveAll(List.of(attendee1, attendee2));

        List<AttendeeDTO> foundAttendees = attendeeService.getAllAttendees();
        assertThat(foundAttendees).isNotNull();
        assertThat(foundAttendees).hasSize(2);
        assertThat(foundAttendees)
                .extracting(AttendeeDTO::email)
                .containsExactlyInAnyOrder("attendeeone@test.com", "attendeetwo@test.com");
        assertThat(foundAttendees)
                .extracting(AttendeeDTO::name)
                .containsExactlyInAnyOrder("Attendee One", "Attendee Two");
    }

    @Test
    void shouldReturnEmptyListWhenNoAttendeesExist() {
        List<AttendeeDTO> attendeesFromRepo = attendeeService.getAllAttendees();
        assertThat(attendeesFromRepo).isNotNull().isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenFindingNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    attendeeService.getAttendeeById(nonExistentAttendeeId);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Update Operations

    @Test
    void shouldUpdateAttendeeSuccessfully() {
        Attendee attendee = new Attendee(
                "Attendee Name",
                "attendeename@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee savedAttendee = attendeeRepository.save(attendee);
        Long attendeeId = savedAttendee.getId();

        UpdateAttendeeRequestDTO requestDTO = new UpdateAttendeeRequestDTO(
                "Attendee Name Updated",
                "attendeenameupdated@test.com",
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        AttendeeDTO updatedAttendeeDTO = attendeeService.updateAttendee(attendeeId, requestDTO);
        assertThat(updatedAttendeeDTO.name()).isEqualTo("Attendee Name Updated");
        assertThat(updatedAttendeeDTO.email()).isEqualTo("attendeenameupdated@test.com");

        Attendee updatedAttendeeFromDb = attendeeRepository.findById(attendeeId)
                .orElseThrow(() -> new AssertionError(("Attendee not found after update")));
        assertThat(updatedAttendeeFromDb.getName()).isEqualTo("Attendee Name Updated");
        assertThat(updatedAttendeeFromDb.getEmail()).isEqualTo("attendeenameupdated@test.com");
        assertThat(updatedAttendeeFromDb.getWorkingStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(updatedAttendeeFromDb.getWorkingEndTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;
        UpdateAttendeeRequestDTO requestDTO = new UpdateAttendeeRequestDTO(
                "Updated name",
                null,
                null,
                null,
                null,
                null
        );
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    attendeeService.updateAttendee(nonExistentAttendeeId, requestDTO);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingAttendeeWithDuplicateEmail() {
        String duplicateEmail = "duplicate@test.com";

        Attendee attendee = new Attendee(
                "Attendee Name",
                "attendeename@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee attendeeToUpdate = attendeeRepository.save(attendee);
        Long attendeeIdToUpdate = attendeeToUpdate.getId();

        Attendee conflictingAttendee = new Attendee(
                "Attendee Name",
                duplicateEmail,
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee savedConflictingAttendee = attendeeRepository.save(conflictingAttendee);
        Long conflictingAttendeeId = savedConflictingAttendee.getId();

        UpdateAttendeeRequestDTO requestDTOWithDuplicateEmail = new UpdateAttendeeRequestDTO(
                null,
                duplicateEmail,
                null,
                null,
                null,
                null
        );

        String expectedErrorMessage = "Another attendee (" + conflictingAttendeeId + ") already exists with email '" + duplicateEmail + "'.";
        IllegalArgumentException thrownException = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    attendeeService.updateAttendee(attendeeIdToUpdate, requestDTOWithDuplicateEmail);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    //  Operations

    @Test
    void shouldDeleteAttendeeSuccessfully() {
        Attendee attendee = new Attendee(
                "Attendee Name",
                "attendeename@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee savedAttendee = attendeeRepository.save(attendee);
        Long attendeeId = savedAttendee.getId();

        assertThat(attendeeRepository.findById(attendeeId)).isPresent();

        attendeeService.deleteAttendee(attendeeId);

        Optional<Attendee> attendeeOptional = attendeeRepository.findById(attendeeId);
        assertThat(attendeeOptional).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    attendeeService.deleteAttendee(nonExistentAttendeeId);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldThrowResourceInUseExceptionWhenDeletingAttendeeWithMeetings() {
        Attendee attendee = new Attendee(
                "Attendee Name",
                "attendeename@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        attendee.setWorkingStartTime(LocalTime.of(7, 30));
        attendee.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee attendeeInUse = attendeeRepository.save(attendee);
        Long attendeeIdToDelete = attendeeInUse.getId();

        Location location = new Location("Room 1", 10);
        location.setWorkingStartTime(LocalTime.of(8, 0));
        location.setWorkingEndTime(LocalTime.of(18, 0));
        Location savedLocation = locationRepository.save(location);

        LocalDate date = LocalDate.of(Year.now().getValue() + 1, 8, 15);
        Meeting meeting = new Meeting(
                "Title",
                date.atTime(11, 0),
                date.atTime(12, 0),
                savedLocation
        );
        meeting.addAttendee(attendeeInUse);
        meetingRepository.save(meeting);

        String expectedErrorMessage = String.format("Attendee cannot be deleted because they are included in %d meeting(s). See details.", 1);
        ResourceInUseException thrownException = assertThrows(
                ResourceInUseException.class,
                () -> {
                    attendeeService.deleteAttendee(attendeeIdToDelete);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
        assertThat(attendeeRepository.findById(attendeeIdToDelete)).isPresent();
    }

    // Concurrent updates testing

    // Optimistic locking test (Repository Level)
    // Note: Service-level optimistic locking tests aren't needed because
    // @Transactional methods always fetch fresh data
    @Test
    void shouldThrowOptimisticLockingException_whenUpdatingStaleMeeting() {
        Attendee initialAttendee = new Attendee(
                "Attendee Name",
                "attendeename@test.com",
                passwordEncoder.encode("password"),
                Role.USER
        );
        initialAttendee.setWorkingStartTime(LocalTime.of(7, 30));
        initialAttendee.setWorkingEndTime(LocalTime.of(16, 30));
        Attendee savedInitialAttendee = attendeeRepository.save(initialAttendee);
        Long attendeeId = savedInitialAttendee.getId();

        // User 1 and User 2 fetch the attendee at the same time
        Attendee attendeeUser1 = attendeeRepository.findById(attendeeId).orElseThrow();
        Attendee attendeeUser2 = attendeeRepository.findById(attendeeId).orElseThrow();

        // Version must be 0 for both users
        assertThat(attendeeUser1.getVersion()).isEqualTo(0);
        assertThat(attendeeUser2.getVersion()).isEqualTo(0);

        // First user updates the attendee
        attendeeUser1.setName("Updated by User 1");
        attendeeRepository.saveAndFlush(attendeeUser1);

        // The attendee version must be 1 now
        Attendee updatedAttendee = attendeeRepository.findById(attendeeId).orElseThrow();
        assertThat(updatedAttendee.getVersion()).isEqualTo(1);
        assertThat(updatedAttendee.getName()).isEqualTo("Updated by User 1");

        // User 2 attempts to update the attendee
        attendeeUser2.setName("Updated by User 2");

        // The update should fail for User 2
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            attendeeRepository.saveAndFlush(attendeeUser2);
        });
    }

    @Test
    void shouldPreserveBothUpdates_whenUsersUpdateDifferentFields() {
        CreateAttendeeRequestDTO createRequest = new CreateAttendeeRequestDTO(
                "Attendee Name",
                "attendeename@test.com",
                "rawPassword",
                Role.USER,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );
        AttendeeDTO initialAttendee = attendeeService.createAttendee(createRequest);
        Long attendeeId = initialAttendee.id();

        // Update 1
        UpdateAttendeeRequestDTO nameUpdate = new UpdateAttendeeRequestDTO(
                "Attendee Name Updated", null, null, null, null, null
        );
        attendeeService.updateAttendee(attendeeId, nameUpdate);

        // Update 2
        UpdateAttendeeRequestDTO emailUpdate = new UpdateAttendeeRequestDTO(
                null, "updated@test.com", null, null, null, null
        );
        attendeeService.updateAttendee(attendeeId, emailUpdate);

        // BOTH changes should succeed
        AttendeeDTO finalAttendee = attendeeService.getAttendeeById(attendeeId);
        assertThat(finalAttendee.name()).isEqualTo("Attendee Name Updated");
        assertThat(finalAttendee.email()).isEqualTo("updated@test.com");
    }

    @Test
    void shouldHandleRapidMultipleUpdates_withoutDataLoss() {
        CreateAttendeeRequestDTO createRequest = new CreateAttendeeRequestDTO(
                "Attendee Name",
                "attendeename@test.com",
                "rawPassword",
                Role.USER,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );
        AttendeeDTO initialAttendee = attendeeService.createAttendee(createRequest);
        Long attendeeId = initialAttendee.id();

        List<String> names = List.of("Update 1", "Update 2", "Update 3", "Final Update");

        for (String name : names) {
            UpdateAttendeeRequestDTO nameUpdate = new UpdateAttendeeRequestDTO(
                    name, null, null, null, null, null
            );
            assertDoesNotThrow(() -> attendeeService.updateAttendee(attendeeId, nameUpdate));
        }

        // The final state should reflect the last update
        AttendeeDTO finalAttendee = attendeeService.getAttendeeById(attendeeId);
        assertThat(finalAttendee.name()).isEqualTo("Final Update");
    }
}
