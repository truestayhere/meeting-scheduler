package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.AbstractIntegrationTest;
import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
import com.truestayhere.meeting_scheduler.exception.MeetingConflictException;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MeetingServiceIntegrationTest extends AbstractIntegrationTest {

    private final LocalDateTime DEFAULT_TIME = LocalDateTime.of(Year.now().getValue() + 1, 8, 15, 10, 0);
    @Autowired
    private MeetingService meetingService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private Location location1, location2;
    private Attendee attendee1, attendee2, attendee3;
    private Meeting meeting1, meeting2;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        attendeeRepository.deleteAll();
        locationRepository.deleteAll();

        location1 = new Location("Room 1", 10);
        location1.setWorkingStartTime(LocalTime.of(8, 0));
        location1.setWorkingEndTime(LocalTime.of(18, 0));
        location2 = new Location("Room 2", 2);
        location2.setWorkingStartTime(LocalTime.of(7, 0));
        location2.setWorkingEndTime(LocalTime.of(17, 0));
        locationRepository.saveAll(List.of(location1, location2));

        attendee1 = new Attendee(
                "Attendee One",
                "attendeeone@test.com",
                passwordEncoder.encode("password1"),
                "ROLE_USER"
        );
        attendee1.setWorkingStartTime(LocalTime.of(9, 0));
        attendee1.setWorkingEndTime(LocalTime.of(17, 0));
        attendee2 = new Attendee(
                "Attendee Two",
                "attendeetwo@test.com",
                passwordEncoder.encode("password2"),
                "ROLE_USER"
        );
        attendee2.setWorkingStartTime(LocalTime.of(9, 0));
        attendee2.setWorkingEndTime(LocalTime.of(17, 0));
        attendee3 = new Attendee(
                "Attendee Three",
                "attendeethree@test.com",
                passwordEncoder.encode("password3"),
                "ROLE_USER"
        );
        attendee3.setWorkingStartTime(LocalTime.of(8, 0));
        attendee3.setWorkingEndTime(LocalTime.of(16, 0));
        attendeeRepository.saveAll(List.of(attendee1, attendee2, attendee3));

        meeting1 = new Meeting(
                "Meeting One Title",
                DEFAULT_TIME,
                DEFAULT_TIME.plusHours(1),
                location1
        );
        meeting1.addAttendee(attendee1);
        meeting2 = new Meeting(
                "Meeting Two Title",
                DEFAULT_TIME.plusMinutes(30),
                DEFAULT_TIME.plusHours(1).minusMinutes(30),
                location2
        );
        meeting2.addAttendee(attendee2);
        meetingRepository.saveAll(List.of(meeting1, meeting2));
    }

    private static Stream<Arguments> invalidMeetingCreationArguments() {
        LocalDateTime validTime = LocalDateTime.of(Year.now().getValue() + 1, 8, 15, 10, 0);
        String validTitle = "Meeting title";

        return Stream.of(
                Arguments.of(
                        "Duplicate Meeting",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime, // Same startTime, endTime and locationId as in existing meeting
                                validTime.plusHours(1),
                                context.location1Id,
                                Set.of(context.attendee2Id)
                        ),
                        IllegalArgumentException.class,
                        "Meeting with the same location, start time and end time already exists"
                ),
                Arguments.of(
                        "Location Conflict",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.plusMinutes(30),
                                validTime.plusHours(1).plusMinutes(30),
                                context.location1Id, // Booked location
                                Set.of(context.attendee2Id)
                        ),
                        MeetingConflictException.class,
                        "Location conflict detected"
                ),
                Arguments.of(
                        "Attendee Conflict",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.plusMinutes(30),
                                validTime.plusHours(1).plusMinutes(30),
                                context.location2Id,
                                Set.of(context.attendee1Id) // Booked attendee
                        ),
                        MeetingConflictException.class,
                        "Attendee conflict detected"
                ),
                Arguments.of(
                        "Exceeds Capacity",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.plusHours(2),
                                validTime.plusHours(3),
                                context.location2Id, // Location with capacity of 2
                                Set.of(context.attendee1Id, context.attendee2Id, context.attendee3Id) // 3 attendees
                        ),
                        IllegalArgumentException.class,
                        "capacity"
                ),
                Arguments.of(
                        "Outside Location Hours",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.minusHours(3),
                                validTime.minusHours(2),
                                context.location1Id, // Location with startTime later
                                Set.of(context.attendee2Id)
                        ),
                        IllegalArgumentException.class,
                        "start time"
                ),
                Arguments.of(
                        "Outside Attendee Hours",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.plusHours(7),
                                validTime.plusHours(8),
                                context.location1Id,
                                Set.of(context.attendee2Id) // Attendee with endTime before
                        ),
                        IllegalArgumentException.class,
                        "end time"
                ),
                Arguments.of(
                        "Non-existent Location",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.plusHours(2),
                                validTime.plusHours(3),
                                0L,
                                Set.of(context.attendee2Id)
                        ),
                        EntityNotFoundException.class,
                        "not found"
                ),
                Arguments.of(
                        "Non-existent Attendee",
                        (TestSetupCallback<CreateMeetingRequestDTO>) context -> new CreateMeetingRequestDTO(
                                validTitle,
                                validTime.plusHours(2),
                                validTime.plusHours(3),
                                context.location1Id,
                                Set.of(0L)
                        ),
                        EntityNotFoundException.class,
                        "not found"
                )
        );
    }

    private static Stream<Arguments> invalidMeetingUpdateArguments() {
        LocalDateTime validTime = LocalDateTime.of(Year.now().getValue() + 1, 8, 15, 10, 0);

        return Stream.of(
                Arguments.of(
                        "Duplicate Meeting",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                validTime.plusMinutes(30), // Same startTime, endTime and locationId as in existing meeting
                                validTime.plusHours(1).minusMinutes(30),
                                context.location2Id,
                                null
                        ),
                        IllegalArgumentException.class,
                        "Meeting with the same location, start time and end time already exists"
                ),
                Arguments.of(
                        "Location Conflict",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                null,
                                null,
                                context.location2Id, // Booked location
                                null
                        ),
                        MeetingConflictException.class,
                        "Location conflict detected"
                ),
                Arguments.of(
                        "Attendee Conflict",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                null,
                                null,
                                null,
                                Set.of(context.attendee2Id) // Booked attendee
                        ),
                        MeetingConflictException.class,
                        "Attendee conflict detected"
                ),
                Arguments.of(
                        "Exceeds Capacity",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                validTime.plusHours(2),
                                validTime.plusHours(3),
                                context.location2Id, // Location with capacity of 2
                                Set.of(context.attendee1Id, context.attendee2Id, context.attendee3Id) // 3 attendees
                        ),
                        IllegalArgumentException.class,
                        "capacity"
                ),
                Arguments.of(
                        "Outside Location Hours",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                validTime.minusHours(3),
                                validTime.minusHours(2),
                                context.location1Id, // Location with startTime later
                                null
                        ),
                        IllegalArgumentException.class,
                        "start time"
                ),
                Arguments.of(
                        "Outside Attendee Hours",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                validTime.plusHours(7),
                                validTime.plusHours(8),
                                context.location1Id,
                                Set.of(context.attendee2Id) // Attendee with endTime before
                        ),
                        IllegalArgumentException.class,
                        "end time"
                ),
                Arguments.of(
                        "Start Time Before End Time",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                null,
                                validTime.minusDays(1),
                                null,
                                null
                        ),
                        IllegalArgumentException.class,
                        "Start time must be before end time"
                ),
                Arguments.of(
                        "Non-existent Location",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                null,
                                null,
                                0L,
                                null
                        ),
                        EntityNotFoundException.class,
                        "not found"
                ),
                Arguments.of(
                        "Non-existent Attendee",
                        (TestSetupCallback<UpdateMeetingRequestDTO>) context -> new UpdateMeetingRequestDTO(
                                null,
                                validTime.plusHours(2),
                                validTime.plusHours(3),
                                null,
                                Set.of(0L)
                        ),
                        EntityNotFoundException.class,
                        "not found"
                )
        );
    }

    // Create Operations

    @Test
    void shouldCreateMeetingSuccessfully() {

        CreateMeetingRequestDTO requestDTO = new CreateMeetingRequestDTO(
                "Meeting title",
                DEFAULT_TIME.plusHours(2),
                DEFAULT_TIME.plusHours(3),
                location1.getId(),
                Set.of(attendee1.getId())
        );

        MeetingDTO createdMeetingDTO = meetingService.createMeeting(requestDTO);
        assertThat(createdMeetingDTO).isNotNull();
        assertThat(createdMeetingDTO.title()).isEqualTo(requestDTO.title());
        assertThat(createdMeetingDTO.startTime()).isEqualTo(requestDTO.startTime());
        assertThat(createdMeetingDTO.endTime()).isEqualTo(requestDTO.endTime());
        assertThat(createdMeetingDTO.location().id()).isEqualTo(requestDTO.locationId());
        assertThat(createdMeetingDTO.attendees()).hasSize(1);
        assertThat(createdMeetingDTO.attendees())
                .extracting("id")
                .containsExactlyInAnyOrder(attendee1.getId());

        Long meetingId = createdMeetingDTO.id();
        MeetingDTO verificationMeetingDTO = meetingService.getMeetingById(meetingId);
        assertThat(verificationMeetingDTO.title()).isEqualTo(requestDTO.title());
        assertThat(verificationMeetingDTO.startTime()).isEqualTo(requestDTO.startTime());
        assertThat(verificationMeetingDTO.endTime()).isEqualTo(requestDTO.endTime());
        assertThat(verificationMeetingDTO.location().id()).isEqualTo(requestDTO.locationId());
        assertThat(verificationMeetingDTO.attendees()).hasSize(1);
        assertThat(verificationMeetingDTO.attendees())
                .extracting("id")
                .containsExactlyInAnyOrder(attendee1.getId());
    }

    @ParameterizedTest(name = "Scenario: {0}")
    @MethodSource("invalidMeetingCreationArguments")
    void shouldThrowException_WhenCreatingInvalidMeeting(
            String testCaseName,
            TestSetupCallback<CreateMeetingRequestDTO> requestBuilder,
            Class<? extends Throwable> expectedException,
            String expectedErrorMessagePart) {

        TestEntityContext context = new TestEntityContext(
                location1.getId(), location2.getId(), attendee1.getId(), attendee2.getId(), attendee3.getId()
        );

        CreateMeetingRequestDTO requestDTO = requestBuilder.build(context);

        Throwable thrownException = assertThrows(expectedException, () -> {
            meetingService.createMeeting(requestDTO);
        });

        assertThat(thrownException.getMessage()).contains(expectedErrorMessagePart);
    }

    // Read Operations

    @Test
    void shouldGetAllMeetingsSuccessfully() {
        List<MeetingDTO> foundMeetings = meetingService.getAllMeetings();
        assertThat(foundMeetings).isNotNull();
        assertThat(foundMeetings).hasSize(2);
        assertThat(foundMeetings)
                .extracting(MeetingDTO::title)
                .containsExactlyInAnyOrder("Meeting One Title", "Meeting Two Title");
    }

    @Test
    void shouldReturnEmptyListWhenNoMeetingsExist() {
        meetingRepository.deleteAll();
        List<MeetingDTO> meetingsFromRepo = meetingService.getAllMeetings();
        assertThat(meetingsFromRepo).isNotNull().isEmpty();
    }

    @Test
    void shouldGetMeetingByIdSuccessfully() {
        Long meetingId = meeting1.getId();

        assertThat(meetingRepository.findById(meetingId)).isPresent();

        MeetingDTO foundMeeting = meetingService.getMeetingById(meetingId);
        assertThat(foundMeeting).isNotNull();
        assertThat(foundMeeting.title()).isEqualTo(meeting1.getTitle());
    }

    @Test
    void shouldThrowEntityNotFoundException_WhenGettingNonExistentMeeting() {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    meetingService.getMeetingById(nonExistentMeetingId);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Update Operations

    @Test
    void shouldUpdateMeetingSuccessfully() {
        Long meetingIdToUpdate = meeting1.getId();

        UpdateMeetingRequestDTO requestDTO = new UpdateMeetingRequestDTO(
                "Title Updated",
                DEFAULT_TIME.plusHours(2),
                DEFAULT_TIME.plusHours(3),
                location2.getId(),
                Set.of(attendee2.getId(), attendee3.getId())
        );

        MeetingDTO updatedMeetingDTO = meetingService.updateMeeting(meetingIdToUpdate, requestDTO);
        assertThat(updatedMeetingDTO).isNotNull();
        assertThat(updatedMeetingDTO.id()).isEqualTo(meetingIdToUpdate);
        assertThat(updatedMeetingDTO.title()).isEqualTo(requestDTO.title());
        assertThat(updatedMeetingDTO.startTime()).isEqualTo(requestDTO.startTime());
        assertThat(updatedMeetingDTO.endTime()).isEqualTo(requestDTO.endTime());
        assertThat(updatedMeetingDTO.location().id()).isEqualTo(requestDTO.locationId());
        assertThat(updatedMeetingDTO.attendees()).hasSize(2);
        assertThat(updatedMeetingDTO.attendees())
                .extracting("id")
                .containsExactlyInAnyOrder(attendee2.getId(), attendee3.getId());

        MeetingDTO verificationMeetingDTO = meetingService.getMeetingById(meetingIdToUpdate);
        assertThat(verificationMeetingDTO.title()).isEqualTo(requestDTO.title());
        assertThat(verificationMeetingDTO.startTime()).isEqualTo(requestDTO.startTime());
        assertThat(verificationMeetingDTO.endTime()).isEqualTo(requestDTO.endTime());
        assertThat(verificationMeetingDTO.location().id()).isEqualTo(requestDTO.locationId());
        assertThat(verificationMeetingDTO.attendees()).hasSize(2);
        assertThat(verificationMeetingDTO.attendees())
                .extracting("id")
                .containsExactlyInAnyOrder(attendee2.getId(), attendee3.getId());
    }

    @ParameterizedTest(name = "Scenario: {0}")
    @MethodSource("invalidMeetingUpdateArguments")
    void shouldThrowException_WhenUpdatingInvalidMeeting(
            String testCaseName,
            TestSetupCallback<UpdateMeetingRequestDTO> requestBuilder,
            Class<? extends Throwable> expectedException,
            String expectedErrorMessagePart) {
        Long meetingIdToUpdate = meeting1.getId();

        TestEntityContext context = new TestEntityContext(
                location1.getId(), location2.getId(), attendee1.getId(), attendee2.getId(), attendee3.getId()
        );

        UpdateMeetingRequestDTO requestDTO = requestBuilder.build(context);

        Throwable thrownException = assertThrows(expectedException, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, requestDTO);
        });

        assertThat(thrownException.getMessage()).contains(expectedErrorMessagePart);
    }

    @Test
    void shouldThrowEntityNotFoundException_WhenUpdatingNonExistentMeeting() {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;

        UpdateMeetingRequestDTO requestDTO = new UpdateMeetingRequestDTO(null, null, null, null, null);

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    meetingService.updateMeeting(nonExistentMeetingId, requestDTO);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Delete Operations

    @Test
    void shouldDeleteMeetingSuccessfully() {
        Long meetingIdToDelete = meeting1.getId();
        assertThat(meetingRepository.findById(meetingIdToDelete)).isPresent();

        meetingService.deleteMeeting(meetingIdToDelete);

        Optional<Meeting> meetingOptional = meetingRepository.findById(meetingIdToDelete);
        assertThat(meetingOptional).isEmpty();
    }

    @Test
    void shouldThrowEntityNotFoundException_WhenDeletingNonExistentMeeting() {
        Long nonExistentMeetingId = 0L;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> {
                    meetingService.deleteMeeting(nonExistentMeetingId);
                }
        );

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // Concurrent updates testing

    // Optimistic locking test (Repository Level)
    // Note: Service-level optimistic locking tests aren't needed because
    // @Transactional methods always fetch fresh data
    @Test
    void shouldThrowOptimisticLockingException_whenUpdatingStaleMeeting() {
        Meeting initialMeeting = new Meeting(
                "Initial Meeting",
                DEFAULT_TIME,
                DEFAULT_TIME.plusHours(1),
                location1
        );
        initialMeeting.addAttendee(attendee1);
        Meeting savedoInitialMeeting = meetingRepository.save(initialMeeting);
        Long meetingId = savedoInitialMeeting.getId();

        // User 1 and User 2 fetch the meeting at the same time
        Meeting meetingUser1 = meetingRepository.findById(meetingId).orElseThrow();
        Meeting meetingUser2 = meetingRepository.findById(meetingId).orElseThrow();

        // Version must be 0 for both users
        assertThat(meetingUser1.getVersion()).isEqualTo(0);
        assertThat(meetingUser2.getVersion()).isEqualTo(0);

        // First user updates the meeting
        meetingUser1.setTitle("Updated by User 1");
        meetingRepository.saveAndFlush(meetingUser1);

        // The meeting version must be 1 now
        Meeting updatedMeeting = meetingRepository.findById(meetingId).orElseThrow();
        assertThat(updatedMeeting.getVersion()).isEqualTo(1);
        assertThat(updatedMeeting.getTitle()).isEqualTo("Updated by User 1");

        // User 2 attempts to update the meeting
        meetingUser2.setTitle("Updated by User 2");

        // The update should fail for User 2
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            meetingRepository.saveAndFlush(meetingUser2);
        });

    }

    @Test
    void shouldPreserveBothUpdates_whenUsersUpdateDifferentFields() {
        CreateMeetingRequestDTO createRequest = new CreateMeetingRequestDTO(
                "Meeting Title",
                DEFAULT_TIME.plusHours(2),
                DEFAULT_TIME.plusHours(3),
                location1.getId(),
                Set.of(attendee1.getId())
        );
        MeetingDTO initialMeeting = meetingService.createMeeting(createRequest);
        Long meetingId = initialMeeting.id();

        // Update 1
        UpdateMeetingRequestDTO titleUpdate = new UpdateMeetingRequestDTO(
                "Meeting Title Updated", null, null, null, null
        );
        meetingService.updateMeeting(meetingId, titleUpdate);

        // Update 2
        UpdateMeetingRequestDTO attendeeUpdate = new UpdateMeetingRequestDTO(
                null, null, null, null, Set.of(attendee1.getId(), attendee2.getId())
        );
        meetingService.updateMeeting(meetingId, attendeeUpdate);

        // BOTH changes should succeed
        MeetingDTO finalMeeting = meetingService.getMeetingById(meetingId);
        assertThat(finalMeeting.title()).isEqualTo("Meeting Title Updated");
        assertThat(finalMeeting.attendees())
                .extracting("id").
                containsExactlyInAnyOrder(
                        attendee1.getId(), attendee2.getId()
                );
    }

    @Test
    void shouldHandleRapidMultipleUpdates_withoutDataLoss() {
        CreateMeetingRequestDTO createRequest = new CreateMeetingRequestDTO(
                "Meeting Title",
                DEFAULT_TIME.plusHours(2),
                DEFAULT_TIME.plusHours(3),
                location1.getId(),
                Set.of(attendee1.getId())
        );
        MeetingDTO initialMeeting = meetingService.createMeeting(createRequest);
        Long meetingId = initialMeeting.id();

        List<String> titles = List.of("Update 1", "Update 2", "Update 3", "Final Update");

        for (String title : titles) {
            UpdateMeetingRequestDTO update = new UpdateMeetingRequestDTO(
                    title, null, null, null, null
            );
            assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, update));
        }

        // The final state should reflect the last update
        MeetingDTO finalMeeting = meetingService.getMeetingById(meetingId);
        assertThat(finalMeeting.title()).isEqualTo("Final Update");
    }

    // Receives test context and populates request
    @FunctionalInterface
    public interface TestSetupCallback<T> {
        T build(TestEntityContext context);
    }

    // Test context (for ParameterizedTest)
    public record TestEntityContext(
            Long location1Id,
            Long location2Id,
            Long attendee1Id,
            Long attendee2Id,
            Long attendee3Id) {
    }
}
