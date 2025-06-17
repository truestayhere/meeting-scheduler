package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.AbstractIntegrationTest;
import com.truestayhere.meeting_scheduler.dto.request.CommonAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.LocationAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.MeetingSuggestionRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AvailableSlotDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationTimeSlotDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AvailabilityServiceIntegrationTest extends AbstractIntegrationTest {

    private final LocalDate DEFAULT_DATE = LocalDate.of(Year.now().getValue() + 1, 8, 14);
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private LocationMapper locationMapper;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private AvailabilityService availabilityService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private Location location1, location2; // capacity 10, works 9-17; capacity 2, works 10-18
    private Attendee attendee1, attendee2, attendee3; // works 9-17, works 9-17, works 10-18

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        locationRepository.deleteAll();
        attendeeRepository.deleteAll();

        location1 = new Location("Room 1", 10);
        location1.setWorkingStartTime(LocalTime.of(9, 0));
        location1.setWorkingEndTime(LocalTime.of(17, 0));
        location1 = locationRepository.save(location1);

        location2 = new Location("Room 2", 2);
        location2.setWorkingStartTime(LocalTime.of(10, 0));
        location2.setWorkingEndTime(LocalTime.of(18, 0));
        location2 = locationRepository.save(location2);

        attendee1 = new Attendee(
                "Attendee One",
                "attendeeone@test.com",
                passwordEncoder.encode("password1")
        );
        attendee1.setWorkingStartTime(LocalTime.of(9, 0));
        attendee1.setWorkingEndTime(LocalTime.of(17, 0));
        attendee1 = attendeeRepository.save(attendee1);

        attendee2 = new Attendee(
                "Attendee Two",
                "attendeetwo@test.com",
                passwordEncoder.encode("password2")
        );
        attendee2.setWorkingStartTime(LocalTime.of(9, 0));
        attendee2.setWorkingEndTime(LocalTime.of(17, 0));
        attendee2 = attendeeRepository.save(attendee2);

        attendee3 = new Attendee(
                "Attendee Three",
                "attendeethree@test.com",
                passwordEncoder.encode("password3")
        );
        attendee3.setWorkingStartTime(LocalTime.of(10, 0));
        attendee3.setWorkingEndTime(LocalTime.of(18, 0));
        attendee3 = attendeeRepository.save(attendee3);
    }

    // getMeetingsFor...

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnCorrectMeetings() {
        LocalDateTime startTime = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime endTime = startTime.plusHours(1);
        createMeetingForAttendee(attendee1.getId(), startTime, endTime);

        List<MeetingDTO> meetings = availabilityService.getMeetingsForAttendeeInRange(
                attendee1.getId(),
                DEFAULT_DATE.atStartOfDay(),
                DEFAULT_DATE.plusDays(1).atStartOfDay()
        );

        assertThat(meetings).hasSize(1);
        assertThat(meetings.get(0).startTime()).isEqualTo(startTime);
    }

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnEmptyList_whenNoMeetingsForAttendeeExist() {
        List<MeetingDTO> meetings = availabilityService.getMeetingsForAttendeeInRange(
                attendee1.getId(),
                DEFAULT_DATE.atStartOfDay(),
                DEFAULT_DATE.plusDays(1).atStartOfDay()
        );

        assertThat(meetings).isEmpty();
    }

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnEmptyList_whenRequestedNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;

        List<MeetingDTO> meetings = availabilityService.getMeetingsForAttendeeInRange(
                nonExistentAttendeeId,
                DEFAULT_DATE.atStartOfDay(),
                DEFAULT_DATE.plusDays(1).atStartOfDay()
        );

        assertThat(meetings).isEmpty();
    }

    @Test
    void getMeetingsForLocationInRange_shouldReturnCorrectMeetings() {
        LocalDateTime startTime = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime endTime = startTime.plusHours(1);
        createMeetingAtLocation(location1.getId(), startTime, endTime);

        List<MeetingDTO> meetings = availabilityService.getMeetingsForLocationInRange(
                location1.getId(),
                DEFAULT_DATE.atStartOfDay(),
                DEFAULT_DATE.plusDays(1).atStartOfDay()
        );

        assertThat(meetings).hasSize(1);
        assertThat(meetings.get(0).startTime()).isEqualTo(startTime);
    }

    @Test
    void getMeetingsForLocationInRange_shouldReturnEmptyList_whenNoMeetingsForLocationExist() {
        List<MeetingDTO> meetings = availabilityService.getMeetingsForLocationInRange(
                location1.getId(),
                DEFAULT_DATE.atStartOfDay(),
                DEFAULT_DATE.plusDays(1).atStartOfDay()
        );

        assertThat(meetings).isEmpty();
    }

    @Test
    void getMeetingsForLocationInRange_shouldReturnEmptyList_whenRequestedNonExistentLocation() {
        Long nonExistentLocationId = 0L;

        List<MeetingDTO> meetings = availabilityService.getMeetingsForLocationInRange(
                nonExistentLocationId,
                DEFAULT_DATE.atStartOfDay(),
                DEFAULT_DATE.plusDays(1).atStartOfDay()
        );

        assertThat(meetings).isEmpty();
    }

    // getAvailableTimeForLocation

    @Test
    void getAvailableTimeForLocation_shouldReturnFullDay_whenNoMeetingsExist() {
        List<AvailableSlotDTO> slots = availabilityService.getAvailableTimeForLocation(
                location1.getId(),
                DEFAULT_DATE
        );

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).startTime().toLocalTime()).isEqualTo(location1.getWorkingStartTime());
        assertThat(slots.get(0).endTime().toLocalTime()).isEqualTo(location1.getWorkingEndTime());
    }

    @Test
    void getAvailableTimeForLocation_shouldReturnTwoSlots_whenOneMeetingIsInMiddle() {
        LocalDateTime startTime = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime endTime = startTime.plusHours(1);
        createMeetingAtLocation(location1.getId(), startTime, endTime);

        List<AvailableSlotDTO> slots = availabilityService.getAvailableTimeForLocation(
                location1.getId(),
                DEFAULT_DATE
        );

        assertThat(slots).hasSize(2);

        // Slot 1: location work start - meeting start
        assertThat(slots.get(0).startTime()).isEqualTo(location1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(slots.get(0).endTime()).isEqualTo(startTime);

        // Slot 2: meeting end - location work end
        assertThat(slots.get(1).startTime()).isEqualTo(endTime);
        assertThat(slots.get(1).endTime()).isEqualTo(location1.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailableTimeForLocation_shouldReturnCorrectSlots_whenLocationBookedForMultipleMeetings() {
        LocalDateTime meeting1Start = DEFAULT_DATE.atTime(10, 0);
        LocalDateTime meeting1End = DEFAULT_DATE.atTime(11, 0);
        LocalDateTime meeting2Start = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime meeting2End = DEFAULT_DATE.atTime(15, 0);
        createMeetingAtLocation(location1.getId(), meeting1Start, meeting1End);
        createMeetingAtLocation(location1.getId(), meeting2Start, meeting2End);

        var slots = availabilityService.getAvailableTimeForLocation(location1.getId(), DEFAULT_DATE);

        assertThat(slots).hasSize(3);

        // Slot 1: location work start - meeting 1 start
        assertThat(slots.get(0).startTime()).isEqualTo(location1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(slots.get(0).endTime()).isEqualTo(meeting1Start);

        // Slot 2: meeting 1 end - meeting 2 start
        assertThat(slots.get(1).startTime()).isEqualTo(meeting1End);
        assertThat(slots.get(1).endTime()).isEqualTo(meeting2Start);

        // Slot 3: meeting 2 end - location work end
        assertThat(slots.get(2).startTime()).isEqualTo(meeting2End);
        assertThat(slots.get(2).endTime()).isEqualTo(location1.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailableTimeForLocation_shouldThrowException_forNonExistentLocation() {
        Long nonExistentLocationId = 0L;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        EntityNotFoundException thrownException = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getAvailableTimeForLocation(nonExistentLocationId, DEFAULT_DATE);
        });

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // getAvailableTimeForAttendee

    @Test
    void getAvailableTimeForAttendee_shouldReturnFullDay_whenNoMeetingsExist() {
        List<AvailableSlotDTO> slots = availabilityService.getAvailableTimeForAttendee(
                attendee1.getId(),
                DEFAULT_DATE
        );

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).startTime().toLocalTime()).isEqualTo(attendee1.getWorkingStartTime());
        assertThat(slots.get(0).endTime().toLocalTime()).isEqualTo(attendee1.getWorkingEndTime());
    }

    @Test
    void getAvailableTimeForAttendee_shouldReturnTwoSlots_whenOneMeetingIsInMiddle() {
        LocalDateTime startTime = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime endTime = startTime.plusHours(1);
        createMeetingForAttendee(attendee1.getId(), startTime, endTime);

        List<AvailableSlotDTO> slots = availabilityService.getAvailableTimeForAttendee(
                attendee1.getId(),
                DEFAULT_DATE
        );

        assertThat(slots).hasSize(2);

        // Slot 1: attendee work start - meeting start
        assertThat(slots.get(0).startTime()).isEqualTo(attendee1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(slots.get(0).endTime()).isEqualTo(startTime);

        // Slot 2: meeting end - attendee work end
        assertThat(slots.get(1).startTime()).isEqualTo(endTime);
        assertThat(slots.get(1).endTime()).isEqualTo(attendee1.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailableTimeForAttendee_shouldReturnCorrectSlots_whenAttendeeIsInMultipleMeetings() {
        LocalDateTime meeting1Start = DEFAULT_DATE.atTime(10, 0);
        LocalDateTime meeting1End = DEFAULT_DATE.atTime(11, 0);
        LocalDateTime meeting2Start = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime meeting2End = DEFAULT_DATE.atTime(15, 0);
        createMeetingForAttendee(attendee1.getId(), meeting1Start, meeting1End);
        createMeetingForAttendee(attendee1.getId(), meeting2Start, meeting2End);

        var slots = availabilityService.getAvailableTimeForAttendee(attendee1.getId(), DEFAULT_DATE);

        assertThat(slots).hasSize(3);

        // Slot 1: attendee work start - meeting 1 start
        assertThat(slots.get(0).startTime()).isEqualTo(attendee1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(slots.get(0).endTime()).isEqualTo(meeting1Start);

        // Slot 2: meeting 1 end - meeting 2 start
        assertThat(slots.get(1).startTime()).isEqualTo(meeting1End);
        assertThat(slots.get(1).endTime()).isEqualTo(meeting2Start);

        // Slot 3: meeting 2 end - attendee work end
        assertThat(slots.get(2).startTime()).isEqualTo(meeting2End);
        assertThat(slots.get(2).endTime()).isEqualTo(attendee1.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailableTimeForAttendee_shouldThrowException_forNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        EntityNotFoundException thrownException = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getAvailableTimeForAttendee(nonExistentAttendeeId, DEFAULT_DATE);
        });

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    // getAvailabilityForLocationsByDuration

    @Test
    void getAvailabilityForLocationsByDuration_shouldReturnSlotsFromAllLocations_whenNoCapacityFilterIsApplied() {
        LocationAvailabilityRequestDTO requestDTO = new LocationAvailabilityRequestDTO(
                DEFAULT_DATE,
                60,
                null
        );

        List<LocationTimeSlotDTO> slots = availabilityService.getAvailabilityForLocationsByDuration(requestDTO);

        assertThat(slots).isNotEmpty();
        assertThat(slots).hasSize(2);

        // Contains full day slots for locations 1 and 2

        LocationTimeSlotDTO location1Slot = slots.stream()
                .filter(slot -> slot.location().id().equals(location1.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Location 1 slot not found"));

        LocationTimeSlotDTO location2Slot = slots.stream()
                .filter(slot -> slot.location().id().equals(location2.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Location 2 slot not found"));

        assertThat(location1Slot.availableSlot().startTime()).isEqualTo(location1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(location1Slot.availableSlot().endTime()).isEqualTo(location1.getWorkingEndTime().atDate(DEFAULT_DATE));

        assertThat(location2Slot.availableSlot().startTime()).isEqualTo(location2.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(location2Slot.availableSlot().endTime()).isEqualTo(location2.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldOnlyReturnSlotsFromLocations_thatMeetCapacityCriteria() {
        LocationAvailabilityRequestDTO requestDTO = new LocationAvailabilityRequestDTO(
                DEFAULT_DATE,
                60,
                5
        );

        List<LocationTimeSlotDTO> slots = availabilityService.getAvailabilityForLocationsByDuration(requestDTO);

        assertThat(slots).isNotEmpty();
        assertThat(slots).hasSize(1);

        // Contains full day slot for location 1

        assertThat(slots.get(0).availableSlot().startTime()).isEqualTo(location1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(slots.get(0).availableSlot().endTime()).isEqualTo(location1.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldOnlyReturnSlots_thatMeetDurationCriteria() {
        LocalDateTime meetingStart = DEFAULT_DATE.atTime(9, 30);
        LocalDateTime meetingEnd = DEFAULT_DATE.atTime(11, 0);

        createMeetingAtLocation(location1.getId(), meetingStart, meetingEnd);

        LocationAvailabilityRequestDTO request = new LocationAvailabilityRequestDTO(
                DEFAULT_DATE,
                60,
                null
        );

        List<LocationTimeSlotDTO> slots = availabilityService.getAvailabilityForLocationsByDuration(request);

        assertThat(slots).isNotEmpty();

        List<LocationTimeSlotDTO> location1Slots = slots.stream()
                .filter(slot -> slot.location().id().equals(location1.getId()))
                .collect(Collectors.toList());

        // location 1 should return 1 slot (meeting end - location 1 working time end)
        assertThat(location1Slots).hasSize(1);
        assertThat(location1Slots.get(0).availableSlot().startTime()).isEqualTo(meetingEnd);
        assertThat(location1Slots.get(0).availableSlot().endTime()).isEqualTo(location1.getWorkingEndTime().atDate(DEFAULT_DATE));
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldThrowException_forNonExistentLocation() {
        Integer greaterCapacity = 20;
        String expectedErrorMessage = "Locations not found with capacity equal or greater than: " + greaterCapacity;

        LocationAvailabilityRequestDTO request = new LocationAvailabilityRequestDTO(
                DEFAULT_DATE,
                60,
                greaterCapacity
        );

        EntityNotFoundException thrownException = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getAvailabilityForLocationsByDuration(request);
        });

        assertThat(thrownException.getMessage()).isEqualTo(expectedErrorMessage);
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldReturnEmptyList_whenLocationsAreFullyBooked() {
        // Create meetings that cover all working hours
        createMeetingAtLocation(location1.getId(), DEFAULT_DATE.atTime(location1.getWorkingStartTime()), DEFAULT_DATE.atTime(location1.getWorkingEndTime()));
        createMeetingAtLocation(location2.getId(), DEFAULT_DATE.atTime(location2.getWorkingStartTime()), DEFAULT_DATE.atTime(location2.getWorkingEndTime()));

        LocationAvailabilityRequestDTO request = new LocationAvailabilityRequestDTO(
                DEFAULT_DATE,
                60,
                null
        );

        List<LocationTimeSlotDTO> slots = availabilityService.getAvailabilityForLocationsByDuration(request);

        assertThat(slots).isEmpty();
    }

    // getCommonAttendeeAvailability

    @Test
    void getCommonAttendeeAvailability_shouldReturnIntersectionOfWorkingHours_whenNoMeetings() {
        CommonAvailabilityRequestDTO requestDTO = new CommonAvailabilityRequestDTO(
                Set.of(attendee1.getId(), attendee3.getId()), // Work 9-17, 10-18
                DEFAULT_DATE
        );

        List<AvailableSlotDTO> slots = availabilityService.getCommonAttendeeAvailability(requestDTO);

        // 1 Common slot: 10-17
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).startTime()).isEqualTo(LocalDateTime.of(DEFAULT_DATE, LocalTime.of(10, 0)));
        assertThat(slots.get(0).endTime()).isEqualTo(LocalDateTime.of(DEFAULT_DATE, LocalTime.of(17, 0)));
    }

    @Test
    void getCommonAttendeeAvailability_shouldReturnIntersection_withMeetings() {
        LocalDateTime meetingStart = DEFAULT_DATE.atTime(11, 0);
        LocalDateTime meetingEnd = DEFAULT_DATE.atTime(12, 0);
        createMeetingForAttendee(attendee1.getId(), meetingStart, meetingEnd);

        CommonAvailabilityRequestDTO requestDTO = new CommonAvailabilityRequestDTO(
                Set.of(attendee1.getId(), attendee2.getId()), // Work 9-17, 9-17
                DEFAULT_DATE
        );

        List<AvailableSlotDTO> slots = availabilityService.getCommonAttendeeAvailability(requestDTO);

        assertThat(slots).hasSize(2);

        // Slot 1: 9-11
        assertThat(slots.get(0).startTime()).isEqualTo(LocalDateTime.of(DEFAULT_DATE, LocalTime.of(9, 0)));
        assertThat(slots.get(0).endTime()).isEqualTo(LocalDateTime.of(DEFAULT_DATE, LocalTime.of(11, 0)));

        // Slot 2: 12-17
        assertThat(slots.get(1).startTime()).isEqualTo(LocalDateTime.of(DEFAULT_DATE, LocalTime.of(12, 0)));
        assertThat(slots.get(1).endTime()).isEqualTo(LocalDateTime.of(DEFAULT_DATE, LocalTime.of(17, 0)));
    }

    @Test
    void getCommonAttendeeAvailability_shouldReturnEmptyList_whenAttendeesHaveNoOverlappingTime() {
        // Meeting that covers all attendee1 and attendee3 common time (10-17)
        createMeetingForAttendee(attendee1.getId(),
                DEFAULT_DATE.atTime(attendee3.getWorkingStartTime()),
                DEFAULT_DATE.atTime(attendee1.getWorkingEndTime())
        );

        CommonAvailabilityRequestDTO requestDTO = new CommonAvailabilityRequestDTO(
                Set.of(attendee1.getId(), attendee3.getId()), // Work 9-17, 10-18
                DEFAULT_DATE
        );

        List<AvailableSlotDTO> slots = availabilityService.getCommonAttendeeAvailability(requestDTO);

        assertThat(slots).isEmpty();
    }

    @Test
    void getCommonAttendeeAvailability_shouldThrowException_forNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;
        CommonAvailabilityRequestDTO request = new CommonAvailabilityRequestDTO(
                Set.of(attendee1.getId(), nonExistentAttendeeId),
                DEFAULT_DATE
        );

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getCommonAttendeeAvailability(request);
        });

        assertThat(exception.getMessage()).matches(expectedErrorMessage);
    }

    // findMeetingSuggestions

    @Test
    void findMeetingSuggestions_shouldSuggestSlots_whenAttendeesAndLocationAreFree() {
        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                Set.of(attendee1.getId(), attendee2.getId()), // Work 9-17, 9-17
                60,
                DEFAULT_DATE
        );

        List<LocationTimeSlotDTO> slots = availabilityService.findMeetingSuggestions(requestDTO);

        assertThat(slots).isNotEmpty();
        assertThat(slots).hasSize(2);

        // Contains 9-17 slot for location1, 10-17 slot for location2

        LocationTimeSlotDTO location1Slot = slots.stream()
                .filter(slot -> slot.location().id().equals(location1.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Location 1 slot not found"));

        LocationTimeSlotDTO location2Slot = slots.stream()
                .filter(slot -> slot.location().id().equals(location2.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Location 2 slot not found"));

        assertThat(location1Slot.availableSlot().startTime()).isEqualTo(location1.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(location1Slot.availableSlot().endTime()).isEqualTo(location1.getWorkingEndTime().atDate(DEFAULT_DATE));

        assertThat(location2Slot.availableSlot().startTime()).isEqualTo(location2.getWorkingStartTime().atDate(DEFAULT_DATE));
        assertThat(location2Slot.availableSlot().endTime()).isEqualTo(DEFAULT_DATE.atTime(17, 0));
    }

    @Test
    void findMeetingSuggestions_shouldSuggestCorrectSlots_whenAttendeesAndLocationHaveFragmentedSchedules() {
        // Book location1 for attendee1 at time 10-11
        LocalDateTime meeting1Start = DEFAULT_DATE.atTime(10, 0);
        LocalDateTime meeting1End = DEFAULT_DATE.atTime(11, 0);
        createMeetingForAttendeesAtLocation(
                List.of(attendee1.getId()),
                location1.getId(),
                meeting1Start,
                meeting1End
        );
        // Book location2 for attendee3 at time 14-15
        LocalDateTime meeting2Start = DEFAULT_DATE.atTime(14, 0);
        LocalDateTime meeting2End = DEFAULT_DATE.atTime(15, 0);
        createMeetingForAttendeesAtLocation(
                List.of(attendee3.getId()),
                location2.getId(),
                meeting2Start,
                meeting2End
        );

        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                Set.of(attendee1.getId(), attendee2.getId()), // Work 9-17, 9-17
                30,
                DEFAULT_DATE
        );

        List<LocationTimeSlotDTO> slots = availabilityService.findMeetingSuggestions(requestDTO);

        assertThat(slots).isNotEmpty();

        // Contains 2 slots for location1: [9-10], [11-17]
        List<LocationTimeSlotDTO> location1Slots = slots.stream()
                .filter(slot -> slot.location().id().equals(location1.getId()))
                .collect(Collectors.toList());
        assertThat(location1Slots).hasSize(2);

        assertThat(location1Slots).containsExactlyInAnyOrder(
                // Slot 1: 9-10
                new LocationTimeSlotDTO(locationMapper.mapToLocationDTO(location1),
                        new AvailableSlotDTO(
                                DEFAULT_DATE.atTime(9, 0),
                                DEFAULT_DATE.atTime(10, 0))),
                // Slot 2: 11-17
                new LocationTimeSlotDTO(locationMapper.mapToLocationDTO(location1),
                        new AvailableSlotDTO(
                                DEFAULT_DATE.atTime(11, 0),
                                DEFAULT_DATE.atTime(17, 0)))
        );

        // Contains 2 slots for location2: [11-14], [15-17]
        List<LocationTimeSlotDTO> location2Slots = slots.stream()
                .filter(slot -> slot.location().id().equals(location2.getId()))
                .collect(Collectors.toList());
        assertThat(location2Slots).hasSize(2);

        assertThat(location2Slots).containsExactlyInAnyOrder(
                // Slot 3: 11-14
                new LocationTimeSlotDTO(locationMapper.mapToLocationDTO(location2),
                        new AvailableSlotDTO(
                                DEFAULT_DATE.atTime(11, 0),
                                DEFAULT_DATE.atTime(14, 0))),
                // Slot4: 15-17
                new LocationTimeSlotDTO(locationMapper.mapToLocationDTO(location2),
                        new AvailableSlotDTO(
                                DEFAULT_DATE.atTime(15, 0),
                                DEFAULT_DATE.atTime(17, 0)))
        );
    }

    @Test
    void findMeetingSuggestions_shouldNotSuggestSlots_whenLocationIsBooked() {
        // Meeting that covers all location1 working time
        createMeetingAtLocation(location1.getId(),
                DEFAULT_DATE.atTime(location1.getWorkingStartTime()),
                DEFAULT_DATE.atTime(location1.getWorkingEndTime())
        );

        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                Set.of(attendee1.getId(), attendee2.getId(), attendee3.getId()), // Location2 capacity doesn't match
                60,
                DEFAULT_DATE
        );

        List<LocationTimeSlotDTO> slots = availabilityService.findMeetingSuggestions(requestDTO);

        assertThat(slots).isEmpty();
    }

    @Test
    void findMeetingSuggestions_shouldReturnEmptyList_whenAttendeesAreNotJointlyAvailable() {
        // Meeting that covers all attendee1 and attendee3 common time (10-17)
        createMeetingForAttendee(attendee1.getId(),
                DEFAULT_DATE.atTime(attendee3.getWorkingStartTime()),
                DEFAULT_DATE.atTime(attendee1.getWorkingEndTime())
        );

        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                Set.of(attendee1.getId(), attendee3.getId()),
                60,
                DEFAULT_DATE
        );

        List<LocationTimeSlotDTO> slots = availabilityService.findMeetingSuggestions(requestDTO);

        assertThat(slots).isEmpty();
    }

    @Test
    void findMeetingSuggestions_shouldReturnEmptyList_whenRequestedDurationIsTooLong() {
        LocalDateTime meetingStart = DEFAULT_DATE.atTime(10, 0);
        LocalDateTime meetingEnd = DEFAULT_DATE.atTime(17, 0);

        // Common free time 9-10
        createMeetingForAttendee(attendee1.getId(),
                meetingStart,
                meetingEnd
        );

        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                Set.of(attendee1.getId(), attendee3.getId()),
                90, // Duration longer that available free time
                DEFAULT_DATE
        );

        List<LocationTimeSlotDTO> slots = availabilityService.findMeetingSuggestions(requestDTO);

        assertThat(slots).isEmpty();
    }

    @Test
    void findMeetingSuggestions_shouldThrowException_forNonExistentAttendee() {
        Long nonExistentAttendeeId = 0L;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;
        MeetingSuggestionRequestDTO requestDTO = new MeetingSuggestionRequestDTO(
                Set.of(attendee1.getId(), nonExistentAttendeeId),
                60,
                DEFAULT_DATE
        );

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.findMeetingSuggestions(requestDTO);
        });

        assertThat(exception.getMessage()).matches(expectedErrorMessage);
    }

    // HELPER METHODS

    // ===== HELPER METHODS FOR TEST DATA CREATION =====

    private Meeting createMeetingForAttendee(Long attendeeId, LocalDateTime startTime, LocalDateTime endTime) {
        Meeting meeting = new Meeting();
        meeting.setTitle("Test Meeting for Attendee " + attendeeId);
        meeting.setStartTime(startTime);
        meeting.setEndTime(endTime);
        meeting.setLocation(location1);

        Attendee attendee = attendeeRepository.findById(attendeeId)
                .orElseThrow(() -> new IllegalArgumentException("Attendee not found with ID: " + attendeeId));
        meeting.setAttendees(Set.of(attendee));

        return meetingRepository.save(meeting);
    }

    private Meeting createMeetingAtLocation(Long locationId, LocalDateTime startTime, LocalDateTime endTime) {
        Meeting meeting = new Meeting();
        meeting.setTitle("Test Meeting at Location " + locationId);
        meeting.setStartTime(startTime);
        meeting.setEndTime(endTime);

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + locationId));
        meeting.setLocation(location);

        meeting.setAttendees(Set.of(attendee1));

        return meetingRepository.save(meeting);
    }

    private Meeting createMeetingForAttendeesAtLocation(List<Long> attendeeIds, Long locationId,
                                                        LocalDateTime startTime, LocalDateTime endTime) {
        Meeting meeting = new Meeting();
        meeting.setTitle("Test Meeting with Multiple Attendees");
        meeting.setStartTime(startTime);
        meeting.setEndTime(endTime);

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + locationId));
        meeting.setLocation(location);

        Set<Attendee> attendees = attendeeIds.stream()
                .map(id -> attendeeRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Attendee not found with ID: " + id)))
                .collect(Collectors.toSet());
        meeting.setAttendees(attendees);

        return meetingRepository.save(meeting);
    }
}
