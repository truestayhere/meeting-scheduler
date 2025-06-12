package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
import com.truestayhere.meeting_scheduler.exception.MeetingConflictException;
import com.truestayhere.meeting_scheduler.mapper.MeetingMapper;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class MeetingServiceTest {

    private final Long DEFAULT_MEETING_ID = 100L;
    private final String DEFAULT_MEETING_TITLE = "Meeting";
    private final LocalDateTime DEFAULT_MEETING_START = LocalDateTime.of(Year.now().getValue() + 1, 8, 4, 13, 0); // Make sure tests does not conflict with meeting time restrictions
    private final LocalDateTime DEFAULT_MEETING_END = LocalDateTime.of(Year.now().getValue() + 1, 8, 4, 14, 0);
    @InjectMocks
    MeetingService meetingService;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AttendeeRepository attendeeRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingMapper meetingMapper;
    private CreateMeetingRequestDTO defaultCreateRequest;
    private UpdateMeetingRequestDTO defaultUpdateRequest;
    private Location mockLocation1, mockLocation2;
    private LocationDTO mockLocationDTO1, mockLocationDTO2;
    private Attendee mockAttendee1, mockAttendee2;
    private AttendeeDTO mockAttendeeDTO1, mockAttendeeDTO2;
    private Set<Attendee> mockAttendees;
    private Set<Long> mockAttendeeIds;
    private Set<AttendeeDTO> mockAttendeeDTOs;
    private Meeting defaultMeeting;
    private Meeting defaultSavedMeeting;
    private MeetingDTO defaultMeetingDTO;


    @BeforeEach
    void setUp() {

        mockLocation1 = new Location();
        mockLocation1.setId(1L);
        mockLocation1.setName("Room 1");
        mockLocation1.setCapacity(10);
        mockLocation1.setWorkingStartTime(LocalTime.of(9, 0));
        mockLocation1.setWorkingEndTime(LocalTime.of(17, 0));

        mockLocation2 = new Location();
        mockLocation2.setId(2L);
        mockLocation2.setName("Room 2");
        mockLocation2.setCapacity(10);
        mockLocation2.setWorkingStartTime(LocalTime.of(8, 0));
        mockLocation2.setWorkingEndTime(LocalTime.of(16, 0));

        mockLocationDTO1 = new LocationDTO(mockLocation1.getId(), mockLocation1.getName(), mockLocation1.getCapacity());
        mockLocationDTO2 = new LocationDTO(mockLocation2.getId(), mockLocation2.getName(), mockLocation2.getCapacity());

        mockAttendee1 = new Attendee();
        mockAttendee1.setId(1L);
        mockAttendee1.setName("Attendee One");
        mockAttendee1.setEmail("attendeeone@test.com");
        mockAttendee1.setWorkingStartTime(LocalTime.of(8, 0));
        mockAttendee1.setWorkingEndTime(LocalTime.of(16, 0));

        mockAttendee2 = new Attendee();
        mockAttendee2.setId(2L);
        mockAttendee2.setName("Attendee Two");
        mockAttendee2.setEmail("attendeetwo@test.com");
        mockAttendee2.setWorkingStartTime(LocalTime.of(9, 0));
        mockAttendee2.setWorkingEndTime(LocalTime.of(17, 0));

        mockAttendees = new HashSet<>(Set.of(mockAttendee1, mockAttendee2));
        mockAttendeeIds = mockAttendees.stream().map(Attendee::getId).collect(Collectors.toSet());

        mockAttendeeDTO1 = new AttendeeDTO(mockAttendee1.getId(), mockAttendee1.getName(), mockAttendee1.getEmail());
        mockAttendeeDTO2 = new AttendeeDTO(mockAttendee2.getId(), mockAttendee2.getName(), mockAttendee2.getEmail());
        mockAttendeeDTOs = Set.of(mockAttendeeDTO1, mockAttendeeDTO2);

        defaultCreateRequest = new CreateMeetingRequestDTO(
                DEFAULT_MEETING_TITLE,
                DEFAULT_MEETING_START,
                DEFAULT_MEETING_END,
                mockLocation1.getId(),
                mockAttendeeIds
        );

        // All fields updated by default
        defaultUpdateRequest = new UpdateMeetingRequestDTO(
                DEFAULT_MEETING_TITLE + " Updated",
                DEFAULT_MEETING_START.minusHours(1),
                DEFAULT_MEETING_END.plusHours(1),
                mockLocation2.getId(),
                Set.of(mockAttendee1.getId())
        );

        defaultMeeting = new Meeting();
        defaultMeeting.setId(DEFAULT_MEETING_ID);
        defaultMeeting.setTitle(DEFAULT_MEETING_TITLE);
        defaultMeeting.setStartTime(DEFAULT_MEETING_START);
        defaultMeeting.setEndTime(DEFAULT_MEETING_END);
        defaultMeeting.setLocation(mockLocation1);
        defaultMeeting.setAttendees(mockAttendees);

        defaultSavedMeeting = new Meeting();
        defaultSavedMeeting.setId(DEFAULT_MEETING_ID);
        defaultSavedMeeting.setTitle(DEFAULT_MEETING_TITLE);
        defaultSavedMeeting.setStartTime(DEFAULT_MEETING_START);
        defaultSavedMeeting.setEndTime(DEFAULT_MEETING_END);
        defaultSavedMeeting.setLocation(mockLocation1);
        defaultSavedMeeting.setAttendees(mockAttendees);

        defaultMeetingDTO = new MeetingDTO(
                DEFAULT_MEETING_ID,
                DEFAULT_MEETING_TITLE,
                DEFAULT_MEETING_START,
                DEFAULT_MEETING_END,
                mockLocationDTO1,
                mockAttendeeDTOs
        );
    }

    // ==== CREATE ====

    @Test
    void createMeeting_shouldReturnMeetingDTO_whenSuccessful() {
        // Fetch location/attendees data
        when(locationRepository.findById(defaultCreateRequest.locationId())).thenReturn(Optional.of(mockLocation1));
        when(attendeeRepository.findAllById(defaultCreateRequest.attendeeIds())).thenReturn(mockAttendees.stream().toList());
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultCreateRequest.locationId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(List.of());
        ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
        when(meetingRepository.save(meetingCaptor.capture())).thenReturn(defaultSavedMeeting);
        when(meetingMapper.mapToMeetingDTO(defaultSavedMeeting)).thenReturn(defaultMeetingDTO);

        MeetingDTO result = meetingService.createMeeting(defaultCreateRequest);

        assertNotNull(result);
        assertEquals(DEFAULT_MEETING_TITLE, result.title());
        assertEquals(DEFAULT_MEETING_START, result.startTime());
        assertEquals(DEFAULT_MEETING_END, result.endTime());
        assertEquals(defaultCreateRequest.locationId(), result.location().id());
        assertNotNull(result.attendees(), "Attendees list in response DTO should not be null.");
        assertEquals(defaultCreateRequest.attendeeIds().size(), result.attendees().size(), "Number of attendees in response should match requested ID count.");

        Set<Long> resultAttendeeIds = result.attendees().stream().map(AttendeeDTO::id).collect(Collectors.toSet());
        assertTrue(resultAttendeeIds.containsAll(defaultCreateRequest.attendeeIds()) &&
                        defaultCreateRequest.attendeeIds().containsAll(resultAttendeeIds),
                "Attendee IDs derived from response DTO do not match the requested IDs.");

        Meeting capturedMeeting = meetingCaptor.getValue();
        assertNotNull(capturedMeeting, "Meeting entity passed to save should not be null.");
        assertEquals(defaultCreateRequest.title(), capturedMeeting.getTitle(), "Title not set correctly on entity for save.");
        assertEquals(defaultCreateRequest.startTime(), capturedMeeting.getStartTime(), "Start time not set correctly.");
        assertEquals(defaultCreateRequest.endTime(), capturedMeeting.getEndTime(), "End time not set correctly.");
        assertSame(mockLocation1, capturedMeeting.getLocation(), "Location object not set correctly.");
        assertNotNull(capturedMeeting.getAttendees(), "Attendees set should not be null.");
        assertEquals(mockAttendees.size(), capturedMeeting.getAttendees().size(), "Number of attendees not set correctly.");
        assertTrue(capturedMeeting.getAttendees().containsAll(mockAttendees) &&
                mockAttendees.containsAll(capturedMeeting.getAttendees()), "Attendees set does not match.");

        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultCreateRequest.locationId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(locationRepository).findById(defaultCreateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultCreateRequest.attendeeIds());
        verify(meetingRepository).save(capturedMeeting);
        verify(meetingMapper).mapToMeetingDTO(defaultSavedMeeting);
    }


    @Test
    void createMeeting_shouldTrowEntityNotFoundException_whenLocationDoesNotExist() {
        Long nonExistentLocationId = mockLocation1.getId();
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;
        when(locationRepository.findById(nonExistentLocationId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            meetingService.createMeeting(defaultCreateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(nonExistentLocationId);
        verify(attendeeRepository, never()).findAllById(anySet());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeAndEndTime(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldTrowEntityNotFoundException_whenAttendeeDoesNotExist() {
        Long existingAttendeeId = mockAttendee1.getId();
        Long nonExistentAttendeeId = 0L;
        Set<Long> setWithNonExistentAttendeeId = Set.of(existingAttendeeId, nonExistentAttendeeId);
        CreateMeetingRequestDTO requestWithNonExistentAttendee = new CreateMeetingRequestDTO(
                defaultCreateRequest.title(),
                defaultCreateRequest.startTime(),
                defaultCreateRequest.endTime(),
                defaultCreateRequest.locationId(),
                setWithNonExistentAttendeeId
        );
        String expectedErrorMessage = "One or more attendees not found.";
        when(locationRepository.findById(requestWithNonExistentAttendee.locationId())).thenReturn(Optional.of(mockLocation1));
        when(attendeeRepository.findAllById(requestWithNonExistentAttendee.attendeeIds())).thenReturn(List.of());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            meetingService.createMeeting(requestWithNonExistentAttendee);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(requestWithNonExistentAttendee.locationId());
        verify(attendeeRepository).findAllById(requestWithNonExistentAttendee.attendeeIds());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeAndEndTime(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowIllegalArgumentException_whenMeetingIsDuplicate() {
        String expectedErrorMessage = "Meeting with the same location, start time and end time already exists.";
        // Fetch location/attendees data
        when(locationRepository.findById(defaultCreateRequest.locationId())).thenReturn(Optional.of(mockLocation1));
        when(attendeeRepository.findAllById(defaultCreateRequest.attendeeIds())).thenReturn(mockAttendees.stream().toList());
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime())).thenReturn(List.of(defaultMeeting));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(defaultCreateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(defaultCreateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultCreateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowMeetingConflictException_whenLocationIsBookedByOtherMeetingsInTimeRequested() {
        Long locationId = mockLocation1.getId();
        Meeting conflictMeeting1 = new Meeting();
        conflictMeeting1.setId(DEFAULT_MEETING_ID + 1);
        Meeting conflictMeeting2 = new Meeting();
        conflictMeeting2.setId(DEFAULT_MEETING_ID + 2);

        List<Meeting> conflictMeetings = List.of(conflictMeeting1, conflictMeeting2);
        String conflictMeetingIds = conflictMeetings.stream()
                .map(m -> m.getId().toString())
                .collect(Collectors.joining(", "));
        String expectedErrorMessage = String.format("Location conflict detected. Location ID %d is booked during the requested time by the meeting(s) with ID(s): %s", locationId, conflictMeetingIds);

        // Fetch location/attendees data
        when(locationRepository.findById(defaultCreateRequest.locationId())).thenReturn(Optional.of(mockLocation1));
        when(attendeeRepository.findAllById(defaultCreateRequest.attendeeIds())).thenReturn(mockAttendees.stream().toList());
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultCreateRequest.locationId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(conflictMeetings);

        MeetingConflictException exception = assertThrows(MeetingConflictException.class, () -> {
            meetingService.createMeeting(defaultCreateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(defaultCreateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultCreateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultCreateRequest.locationId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowMeetingConflictException_whenAttendeeIsBookedByOtherMeetingsInTimeRequested() {
        Long attendeeId = mockAttendee2.getId();
        Meeting conflictMeeting1 = new Meeting();
        conflictMeeting1.setId(DEFAULT_MEETING_ID + 1);
        Meeting conflictMeeting2 = new Meeting();
        conflictMeeting2.setId(DEFAULT_MEETING_ID + 2);

        List<Meeting> conflictMeetings = List.of(conflictMeeting1, conflictMeeting2);
        String conflictMeetingIds = conflictMeetings.stream()
                .map(m -> m.getId().toString())
                .collect(Collectors.joining(", "));
        String expectedErrorMessage = String.format("Attendee conflict detected. Attendee ID %d is already booked during the requested time by meeting(s) with ID(s): %s", attendeeId, conflictMeetingIds);

        // Fetch location/attendees data
        when(locationRepository.findById(defaultCreateRequest.locationId())).thenReturn(Optional.of(mockLocation1));
        when(attendeeRepository.findAllById(defaultCreateRequest.attendeeIds())).thenReturn(mockAttendees.stream().toList());
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultCreateRequest.locationId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime())).thenReturn(conflictMeetings);


        MeetingConflictException exception = assertThrows(MeetingConflictException.class, () -> {
            meetingService.createMeeting(defaultCreateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(defaultCreateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultCreateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultCreateRequest.locationId(), defaultCreateRequest.startTime(), defaultCreateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultCreateRequest.locationId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), defaultCreateRequest.endTime(), defaultCreateRequest.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowIllegalArgumentException_whenMeetingStartsBeforeLocationWorkingStartTime() {
        LocalDate meetingDate = defaultCreateRequest.startTime().toLocalDate();

        Location conflictingLocation = new Location();
        conflictingLocation.setId(mockLocation1.getId());
        conflictingLocation.setWorkingStartTime(mockLocation1.getWorkingStartTime());
        conflictingLocation.setWorkingEndTime(mockLocation1.getWorkingEndTime());

        LocalDateTime locationWorkStartAtDate = meetingDate.atTime(conflictingLocation.getWorkingStartTime());
        LocalDateTime meetingStartBeforeLocationWorkStart = locationWorkStartAtDate.minusHours(1);
        CreateMeetingRequestDTO requestWithTimeConflict = new CreateMeetingRequestDTO(
                defaultCreateRequest.title(),
                meetingStartBeforeLocationWorkStart,
                defaultCreateRequest.endTime(),
                conflictingLocation.getId(),
                defaultCreateRequest.attendeeIds()
        );
        String expectedErrorMessage = String.format("Meeting start time (%s) is before location's working start time (%s).", requestWithTimeConflict.startTime(), locationWorkStartAtDate);
        // Fetch location/attendees data
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(conflictingLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(mockAttendees.stream().toList());
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowIllegalArgumentException_whenMeetingEndsAfterLocationWorkingEndTime() {
        LocalDate meetingDate = defaultCreateRequest.endTime().toLocalDate();

        Location conflictingLocation = new Location();
        conflictingLocation.setId(mockLocation1.getId());
        conflictingLocation.setWorkingStartTime(mockLocation1.getWorkingStartTime());
        conflictingLocation.setWorkingEndTime(mockLocation1.getWorkingEndTime());

        LocalDateTime locationWorkEndAtDate = meetingDate.atTime(conflictingLocation.getWorkingEndTime());
        LocalDateTime meetingEndAfterLocationWorkEnd = locationWorkEndAtDate.plusHours(1);
        CreateMeetingRequestDTO requestWithTimeConflict = new CreateMeetingRequestDTO(
                defaultCreateRequest.title(),
                defaultCreateRequest.startTime(),
                meetingEndAfterLocationWorkEnd,
                conflictingLocation.getId(),
                defaultCreateRequest.attendeeIds()
        );
        String expectedErrorMessage = String.format("Meeting end time (%s) is after location's working end time (%s).", meetingEndAfterLocationWorkEnd, locationWorkEndAtDate);
        // Fetch location/attendees data
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(conflictingLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(mockAttendees.stream().toList());
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowIllegalArgumentException_whenMeetingStartsBeforeAttendeeWorkingStartTime() {
        Attendee conflictAttendee = new Attendee();

        conflictAttendee.setId(mockAttendee1.getId());
        conflictAttendee.setEmail(mockAttendee1.getEmail());
        conflictAttendee.setWorkingStartTime(defaultCreateRequest.startTime().toLocalTime());

        LocalDate meetingDate = defaultCreateRequest.startTime().toLocalDate();
        LocalDateTime attendeeWorkStartAtDate = meetingDate.atTime(conflictAttendee.getWorkingStartTime());
        LocalDateTime meetingStartBeforeAttendeeWorkStart = attendeeWorkStartAtDate.minusHours(1);

        Location nonConflictLocation = new Location();
        nonConflictLocation.setId(mockLocation1.getId());
        nonConflictLocation.setName(mockLocation1.getName());
        nonConflictLocation.setCapacity(mockLocation1.getCapacity());
        nonConflictLocation.setWorkingStartTime(meetingStartBeforeAttendeeWorkStart.toLocalTime());

        CreateMeetingRequestDTO requestWithTimeConflict = new CreateMeetingRequestDTO(
                defaultCreateRequest.title(),
                meetingStartBeforeAttendeeWorkStart,
                defaultCreateRequest.endTime(),
                nonConflictLocation.getId(),
                Set.of(conflictAttendee.getId())
        );
        String expectedErrorMessage = String.format("Meeting start time (%s) is before attendee ID: %d working start time (%s).", meetingStartBeforeAttendeeWorkStart, conflictAttendee.getId(), attendeeWorkStartAtDate);

        // Fetch location/attendees data
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(nonConflictLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(List.of(conflictAttendee));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                conflictAttendee.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                conflictAttendee.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowIllegalArgumentException_whenMeetingEndsAfterAttendeeWorkingEndTime() {
        Attendee conflictAttendee = new Attendee();
        conflictAttendee.setId(mockAttendee1.getId());
        conflictAttendee.setEmail(mockAttendee1.getEmail());
        conflictAttendee.setWorkingEndTime(defaultCreateRequest.endTime().toLocalTime());

        LocalDate meetingDate = defaultCreateRequest.endTime().toLocalDate();
        LocalDateTime attendeeWorkEndAtDate = meetingDate.atTime(conflictAttendee.getWorkingEndTime());
        LocalDateTime meetingEndAfterAttendeeWorkEnd = attendeeWorkEndAtDate.plusHours(1);

        Location nonConflictLocation = new Location();
        nonConflictLocation.setId(mockLocation1.getId());
        nonConflictLocation.setName(mockLocation1.getName());
        nonConflictLocation.setCapacity(mockLocation1.getCapacity());
        nonConflictLocation.setWorkingEndTime(meetingEndAfterAttendeeWorkEnd.toLocalTime());

        CreateMeetingRequestDTO requestWithTimeConflict = new CreateMeetingRequestDTO(
                defaultCreateRequest.title(),
                defaultCreateRequest.startTime(),
                meetingEndAfterAttendeeWorkEnd,
                nonConflictLocation.getId(),
                Set.of(conflictAttendee.getId())
        );
        String expectedErrorMessage = String.format("Meeting end time (%s) is after attendee ID: %d working end time (%s).", meetingEndAfterAttendeeWorkEnd, conflictAttendee.getId(), attendeeWorkEndAtDate);

        // Fetch location/attendees data
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(nonConflictLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(List.of(conflictAttendee));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                conflictAttendee.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                conflictAttendee.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void createMeeting_shouldThrowIllegalArgumentException_whenLocationCapacityIsLessThanNumberOfAttendees() {
        Location locationWithLessCapacity = new Location();
        locationWithLessCapacity.setId(mockLocation1.getId());
        locationWithLessCapacity.setName(mockLocation1.getName());
        locationWithLessCapacity.setCapacity(1);

        List<Attendee> moreAttendees = List.of(mockAttendee1, mockAttendee2);
        Set<Long> moreAttendeeIds = Set.of(mockAttendee1.getId(), mockAttendee2.getId()); // Make sure that attendees in request > 1

        CreateMeetingRequestDTO createRequestWithLessCapacity = new CreateMeetingRequestDTO(
                defaultCreateRequest.title(),
                defaultCreateRequest.startTime(),
                defaultCreateRequest.endTime(),
                locationWithLessCapacity.getId(),
                moreAttendeeIds
        );

        String expectedErrorMessage = String.format("Meeting cannot be created. Location '%s' (ID: %d) has a capacity of %d, but %d attendees were invited.",
                locationWithLessCapacity.getName(),
                locationWithLessCapacity.getId(),
                locationWithLessCapacity.getCapacity(),
                createRequestWithLessCapacity.attendeeIds().size());

        // Fetch location/attendees data
        when(locationRepository.findById(createRequestWithLessCapacity.locationId())).thenReturn(Optional.of(locationWithLessCapacity));
        when(attendeeRepository.findAllById(createRequestWithLessCapacity.attendeeIds())).thenReturn(moreAttendees);
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                createRequestWithLessCapacity.locationId(), createRequestWithLessCapacity.startTime(), createRequestWithLessCapacity.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                createRequestWithLessCapacity.locationId(), createRequestWithLessCapacity.endTime(), createRequestWithLessCapacity.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), createRequestWithLessCapacity.endTime(), createRequestWithLessCapacity.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), createRequestWithLessCapacity.endTime(), createRequestWithLessCapacity.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(createRequestWithLessCapacity);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(locationWithLessCapacity.getId());
        verify(attendeeRepository).findAllById(moreAttendeeIds);
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                createRequestWithLessCapacity.locationId(), createRequestWithLessCapacity.startTime(), createRequestWithLessCapacity.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                createRequestWithLessCapacity.locationId(), createRequestWithLessCapacity.endTime(), createRequestWithLessCapacity.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), createRequestWithLessCapacity.endTime(), createRequestWithLessCapacity.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), createRequestWithLessCapacity.endTime(), createRequestWithLessCapacity.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }

    // ==== END CREATE ====

    // ==== GET ====

    @Test
    void getAllMeetings_shouldReturnListOfMeetingDTOs_whenMeetingsExist() {
        Meeting meeting1 = new Meeting();
        meeting1.setId(defaultMeeting.getId());
        meeting1.setTitle(defaultMeeting.getTitle());

        Meeting meeting2 = new Meeting();
        meeting2.setId(defaultMeeting.getId() + 1);
        meeting2.setTitle(defaultMeeting.getTitle() + " (2)");

        MeetingDTO dto1 = new MeetingDTO(meeting1.getId(), meeting1.getTitle(), null, null, null, null);
        MeetingDTO dto2 = new MeetingDTO(meeting1.getId(), meeting1.getTitle(), null, null, null, null);

        List<Meeting> mockMeetings = List.of(meeting1, meeting2);
        List<MeetingDTO> mockMeetingDTOs = List.of(dto1, dto2);

        when(meetingRepository.findAll()).thenReturn(mockMeetings);
        when(meetingMapper.mapToMeetingDTOList(mockMeetings)).thenReturn(mockMeetingDTOs);

        List<MeetingDTO> results = meetingService.getAllMeetings();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsAll(mockMeetingDTOs)
                && mockMeetingDTOs.containsAll(results));

        verify(meetingRepository).findAll();
        verify(meetingMapper).mapToMeetingDTOList(mockMeetings);
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void getAllMeetings_shouldReturnEmptyList_whenNoMeetingsExist() {
        List<Meeting> emptyMeetingsList = List.of();
        List<MeetingDTO> emptyDTOList = List.of();

        when(meetingRepository.findAll()).thenReturn(emptyMeetingsList);
        when(meetingMapper.mapToMeetingDTOList(emptyMeetingsList)).thenReturn(emptyDTOList);

        List<MeetingDTO> results = meetingService.getAllMeetings();

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(meetingRepository).findAll();
        verify(meetingMapper).mapToMeetingDTOList(emptyMeetingsList);
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }

    @Test
    void getMeetingById_shouldReturnMeetingDTO_whenMeetingExists() {
        Long meetingId = defaultMeeting.getId();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(defaultMeeting));
        when(meetingMapper.mapToMeetingDTO(defaultMeeting)).thenReturn(defaultMeetingDTO);

        MeetingDTO result = meetingService.getMeetingById(meetingId);

        assertNotNull(result);
        assertEquals(defaultMeeting.getId(), result.id());
        assertEquals(defaultMeeting.getTitle(), result.title());
        assertEquals(defaultMeeting.getStartTime(), result.startTime());
        assertEquals(defaultMeeting.getEndTime(), result.endTime());
        assertEquals(defaultMeeting.getLocation().getId(), result.location().id());
        assertEquals(defaultMeeting.getAttendees().size(), result.attendees().size());

        verify(meetingRepository).findById(meetingId);
        verify(meetingMapper).mapToMeetingDTO(defaultMeeting);
    }

    @Test
    void getMeetingById_shouldThrowEntityNotFoundException_whenMeetingDoesNotExist() {
        Long nonExistentMeetingId = DEFAULT_MEETING_ID;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;
        when(meetingRepository.findById(nonExistentMeetingId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> meetingService.getMeetingById(nonExistentMeetingId)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(nonExistentMeetingId);
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }

    // ==== END GET ====

    // ==== UPDATE ====

    @Test
    void updateMeeting_shouldReturnMeetingDTO_whenSomeFieldsUpdated() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        UpdateMeetingRequestDTO updateRequest = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                null,
                defaultUpdateRequest.endTime(),
                defaultUpdateRequest.locationId(),
                null
        );

        // Fetch data (attendees are NOT being updated)
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(updateRequest.locationId())).thenReturn(Optional.of(mockLocation2));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                updateRequest.locationId(), defaultMeeting.getStartTime(), updateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                updateRequest.locationId(), updateRequest.endTime(), defaultMeeting.getStartTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), updateRequest.endTime(), defaultMeeting.getStartTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), updateRequest.endTime(), defaultMeeting.getStartTime())).thenReturn(List.of());

        MeetingDTO expectedResponse = new MeetingDTO(
                meetingIdToUpdate,
                updateRequest.title(),
                defaultMeeting.getStartTime(),
                updateRequest.endTime(),
                mockLocationDTO2,
                mockAttendeeDTOs
        );

        when(meetingMapper.mapToMeetingDTO(any(Meeting.class))).thenReturn(expectedResponse);

        MeetingDTO result = meetingService.updateMeeting(meetingIdToUpdate, updateRequest);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingMapper).mapToMeetingDTO(meetingCaptor.capture());
        Meeting capturedMeeting = meetingCaptor.getValue();
        assertNotNull(capturedMeeting);
        assertEquals(defaultUpdateRequest.title(), capturedMeeting.getTitle());
        assertEquals(defaultMeeting.getStartTime(), capturedMeeting.getStartTime(), "Start time should not have changed.");
        assertEquals(updateRequest.endTime(), capturedMeeting.getEndTime(), "End time should have been updated.");
        assertEquals(mockLocation2.getId(), capturedMeeting.getLocation().getId(), "Location should have been updated.");
        assertEquals(defaultMeeting.getAttendees(), capturedMeeting.getAttendees(), "Attendees should not have changed.");

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(updateRequest.locationId());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                updateRequest.locationId(), defaultMeeting.getStartTime(), updateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                updateRequest.locationId(), updateRequest.endTime(), defaultMeeting.getStartTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), updateRequest.endTime(), defaultMeeting.getStartTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), updateRequest.endTime(), defaultMeeting.getStartTime());
    }

    @Test
    void updateMeeting_shouldReturnMeetingDTO_whenAllFieldsUpdated() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        // Use default update request

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(defaultUpdateRequest.locationId())).thenReturn(Optional.of(mockLocation2));
        when(attendeeRepository.findAllById(defaultUpdateRequest.attendeeIds())).thenReturn(List.of(mockAttendee1));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime())).thenReturn(List.of());

        MeetingDTO expectedResponse = new MeetingDTO(
                meetingIdToUpdate,
                defaultUpdateRequest.title(),
                defaultUpdateRequest.startTime(),
                defaultUpdateRequest.endTime(),
                mockLocationDTO2,
                Set.of(mockAttendeeDTO1)
        );
        when(meetingMapper.mapToMeetingDTO(any(Meeting.class))).thenReturn(expectedResponse);

        MeetingDTO result = meetingService.updateMeeting(meetingIdToUpdate, defaultUpdateRequest);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingMapper).mapToMeetingDTO(meetingCaptor.capture());
        Meeting capturedMeeting = meetingCaptor.getValue();
        assertNotNull(capturedMeeting);
        assertEquals(defaultUpdateRequest.title(), capturedMeeting.getTitle());
        assertEquals(defaultUpdateRequest.startTime(), capturedMeeting.getStartTime());
        assertEquals(defaultUpdateRequest.endTime(), capturedMeeting.getEndTime());
        assertEquals(mockLocation2, capturedMeeting.getLocation());
        assertEquals(1, capturedMeeting.getAttendees().size());
        assertTrue(capturedMeeting.getAttendees().contains(mockAttendee1));

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(defaultUpdateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultUpdateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime());
    }

    @Test
    void updateMeeting_shouldSucceed_whenDuplicatesAndConflictChecksReturnsOnlyTheMeetingBeingUpdated() {
        Long meetingIdToUpdate = defaultMeeting.getId();
        UpdateMeetingRequestDTO updateRequest = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                null,
                null,
                null,
                null
        );

        // Fetch data (location and attendees are NOT being updated)
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        // End fetch data
        // Return the meeting itself on duplicate check
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultMeeting.getLocation().getId(), defaultMeeting.getStartTime(), defaultMeeting.getEndTime())).thenReturn(List.of(defaultMeeting));
        // Return the meeting itself on location conflict check
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultMeeting.getLocation().getId(), defaultMeeting.getEndTime(), defaultMeeting.getStartTime())).thenReturn(List.of(defaultMeeting));
        // Return the meeting itself on attendee conflict check
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultMeeting.getEndTime(), defaultMeeting.getStartTime())).thenReturn(List.of(defaultMeeting));

        MeetingDTO expectedResponse = new MeetingDTO(
                meetingIdToUpdate, updateRequest.title(), null, null , null, null
        );
        when(meetingMapper.mapToMeetingDTO(any(Meeting.class))).thenReturn(expectedResponse);

        assertDoesNotThrow(() -> {
            meetingService.updateMeeting(meetingIdToUpdate, updateRequest);
        }, "Update should succeed when duplicates & attendee conflict & location conflict checks only find the meeting being updated itself.");

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultMeeting.getLocation().getId(), defaultMeeting.getStartTime(), defaultMeeting.getEndTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultMeeting.getLocation().getId(), defaultMeeting.getEndTime(), defaultMeeting.getStartTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), defaultMeeting.getEndTime(), defaultMeeting.getStartTime());
    }


    @Test
    void updateMeeting_shouldThrowEntityNotFoundException_whenMeetingDoesNotExist() {
        Long nonExistentMeetingId = DEFAULT_MEETING_ID;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;

        when(meetingRepository.findById(nonExistentMeetingId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            meetingService.updateMeeting(nonExistentMeetingId, defaultUpdateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(nonExistentMeetingId);
        verify(locationRepository, never()).findById(anyLong());
        verify(attendeeRepository, never()).findAllById(anySet());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeAndEndTime(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldTrowEntityNotFoundException_whenLocationDoesNotExist() {
        Long meetingIdToUpdate = defaultMeeting.getId();
        Long nonExistentLocationId = defaultUpdateRequest.locationId();
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(nonExistentLocationId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, defaultUpdateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(nonExistentLocationId);
        verify(attendeeRepository, never()).findAllById(anySet());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeAndEndTime(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldTrowEntityNotFoundException_whenAttendeeDoesNotExist() {
        Long meetingIdToUpdate = defaultMeeting.getId();
        Long nonExistentAttendeeId = mockAttendee1.getId();
        Set<Long> setWithNonExistentAttendeeId = Set.of(nonExistentAttendeeId);
        UpdateMeetingRequestDTO requestWithNonExistentAttendee = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                defaultUpdateRequest.startTime(),
                defaultUpdateRequest.endTime(),
                defaultUpdateRequest.locationId(),
                setWithNonExistentAttendeeId
        );
        String expectedErrorMessage = "One or more attendees not found.";
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(requestWithNonExistentAttendee.locationId())).thenReturn(Optional.of(mockLocation2));
        when(attendeeRepository.findAllById(requestWithNonExistentAttendee.attendeeIds())).thenReturn(List.of());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, requestWithNonExistentAttendee);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(requestWithNonExistentAttendee.locationId());
        verify(attendeeRepository).findAllById(requestWithNonExistentAttendee.attendeeIds());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeAndEndTime(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowIllegalArgumentException_whenMeetingIsDuplicate() {
        Long meetingIdToUpdate = DEFAULT_MEETING_ID;
        Meeting anotherMeeting = new Meeting();
        anotherMeeting.setId(defaultMeeting.getId() + 1);
        anotherMeeting.setStartTime(defaultMeeting.getStartTime());
        anotherMeeting.setEndTime(defaultMeeting.getEndTime());
        anotherMeeting.setLocation(defaultMeeting.getLocation());
        String expectedErrorMessage = "Meeting with the same location, start time and end time already exists.";
        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(defaultUpdateRequest.locationId())).thenReturn(Optional.of(mockLocation2));
        when(attendeeRepository.findAllById(defaultUpdateRequest.attendeeIds())).thenReturn(List.of(mockAttendee1));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime())).thenReturn(List.of(anotherMeeting));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, defaultUpdateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(defaultUpdateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultUpdateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime());
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowMeetingConflictException_whenLocationIsBookedByOtherMeetingsInTimeRequested() {
        Long meetingIdToUpdate = DEFAULT_MEETING_ID;
        Long locationId = defaultUpdateRequest.locationId();
        Meeting conflictMeeting1 = new Meeting();
        conflictMeeting1.setId(DEFAULT_MEETING_ID + 1);
        Meeting conflictMeeting2 = new Meeting();
        conflictMeeting2.setId(DEFAULT_MEETING_ID + 2);

        List<Meeting> conflictMeetings = List.of(conflictMeeting1, conflictMeeting2);
        String conflictMeetingIds = conflictMeetings.stream()
                .map(m -> m.getId().toString())
                .collect(Collectors.joining(", "));
        String expectedErrorMessage = String.format("Location conflict detected. Location ID %d is booked during the requested time by the meeting(s) with ID(s): %s", locationId, conflictMeetingIds);

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(defaultUpdateRequest.locationId())).thenReturn(Optional.of(mockLocation2));
        when(attendeeRepository.findAllById(defaultUpdateRequest.attendeeIds())).thenReturn(List.of(mockAttendee1));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime())).thenReturn(conflictMeetings);

        MeetingConflictException exception = assertThrows(MeetingConflictException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, defaultUpdateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(defaultUpdateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultUpdateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime());
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowMeetingConflictException_whenAttendeeIsBookedByOtherMeetingsInTimeRequested() {
        Long meetingIdToUpdate = DEFAULT_MEETING_ID;
        Long attendeeId = mockAttendee1.getId();
        Meeting conflictMeeting1 = new Meeting();
        conflictMeeting1.setId(DEFAULT_MEETING_ID + 1);
        Meeting conflictMeeting2 = new Meeting();
        conflictMeeting2.setId(DEFAULT_MEETING_ID + 2);

        List<Meeting> conflictMeetings = List.of(conflictMeeting1, conflictMeeting2);
        String conflictMeetingIds = conflictMeetings.stream()
                .map(m -> m.getId().toString())
                .collect(Collectors.joining(", "));
        String expectedErrorMessage = String.format("Attendee conflict detected. Attendee ID %d is already booked during the requested time by meeting(s) with ID(s): %s", attendeeId, conflictMeetingIds);

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(defaultUpdateRequest.locationId())).thenReturn(Optional.of(mockLocation2));
        when(attendeeRepository.findAllById(defaultUpdateRequest.attendeeIds())).thenReturn(List.of(mockAttendee1));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                attendeeId, defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime())).thenReturn(conflictMeetings);

        MeetingConflictException exception = assertThrows(MeetingConflictException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, defaultUpdateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(defaultUpdateRequest.locationId());
        verify(attendeeRepository).findAllById(defaultUpdateRequest.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.startTime(), defaultUpdateRequest.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                defaultUpdateRequest.locationId(), defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                attendeeId, defaultUpdateRequest.endTime(), defaultUpdateRequest.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowIllegalArgumentException_whenMeetingStartsBeforeLocationWorkingStartTime() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        Location conflictingLocation = new Location();
        conflictingLocation.setId(mockLocation1.getId());
        conflictingLocation.setWorkingStartTime(mockLocation1.getWorkingStartTime());
        conflictingLocation.setWorkingEndTime(mockLocation1.getWorkingEndTime());

        LocalDate meetingDate = defaultUpdateRequest.startTime().toLocalDate();
        LocalDateTime locationWorkStartAtDate = meetingDate.atTime(conflictingLocation.getWorkingStartTime());
        LocalDateTime meetingStartBeforeLocationWorkStart = locationWorkStartAtDate.minusHours(1);
        UpdateMeetingRequestDTO requestWithTimeConflict = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                meetingStartBeforeLocationWorkStart,
                defaultUpdateRequest.endTime(),
                conflictingLocation.getId(),
                defaultUpdateRequest.attendeeIds()
        );
        String expectedErrorMessage = String.format("Meeting start time (%s) is before location's working start time (%s).", requestWithTimeConflict.startTime(), locationWorkStartAtDate);

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(conflictingLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(List.of(mockAttendee1));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowIllegalArgumentException_whenMeetingEndsAfterLocationWorkingEndTime() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        Location conflictingLocation = new Location();
        conflictingLocation.setId(mockLocation1.getId());
        conflictingLocation.setWorkingStartTime(mockLocation1.getWorkingStartTime());
        conflictingLocation.setWorkingEndTime(mockLocation1.getWorkingEndTime());

        LocalDate meetingDate = defaultUpdateRequest.startTime().toLocalDate();
        LocalDateTime locationWorkEndAtDate = meetingDate.atTime(conflictingLocation.getWorkingEndTime());
        LocalDateTime meetingEndAfterLocationWorkEnd = locationWorkEndAtDate.plusHours(1);
        UpdateMeetingRequestDTO requestWithTimeConflict = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                defaultUpdateRequest.startTime(),
                meetingEndAfterLocationWorkEnd,
                conflictingLocation.getId(),
                defaultUpdateRequest.attendeeIds()
        );
        String expectedErrorMessage = String.format("Meeting end time (%s) is after location's working end time (%s).", meetingEndAfterLocationWorkEnd, locationWorkEndAtDate);

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(conflictingLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(List.of(mockAttendee1));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowIllegalArgumentException_whenMeetingStartsBeforeAttendeeWorkingStartTime() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        Attendee conflictAttendee = new Attendee();
        conflictAttendee.setId(mockAttendee1.getId());
        conflictAttendee.setEmail(mockAttendee1.getEmail());
        conflictAttendee.setWorkingStartTime(defaultUpdateRequest.startTime().toLocalTime());

        LocalDate meetingDate = defaultUpdateRequest.startTime().toLocalDate();
        LocalDateTime attendeeWorkStartAtDate = meetingDate.atTime(conflictAttendee.getWorkingStartTime());
        LocalDateTime meetingStartBeforeAttendeeWorkStart = attendeeWorkStartAtDate.minusHours(1);

        Location nonConflictLocation = new Location();
        nonConflictLocation.setId(mockLocation1.getId());
        nonConflictLocation.setWorkingStartTime(meetingStartBeforeAttendeeWorkStart.toLocalTime());

        UpdateMeetingRequestDTO requestWithTimeConflict = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                meetingStartBeforeAttendeeWorkStart,
                defaultUpdateRequest.endTime(),
                nonConflictLocation.getId(),
                Set.of(conflictAttendee.getId())
        );
        String expectedErrorMessage = String.format("Meeting start time (%s) is before attendee ID: %d working start time (%s).", meetingStartBeforeAttendeeWorkStart, conflictAttendee.getId(), attendeeWorkStartAtDate);

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(nonConflictLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(List.of(conflictAttendee));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowIllegalArgumentException_whenMeetingEndsAfterAttendeeWorkingEndTime() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        Attendee conflictAttendee = new Attendee();
        conflictAttendee.setId(mockAttendee1.getId());
        conflictAttendee.setEmail(mockAttendee1.getEmail());
        conflictAttendee.setWorkingEndTime(defaultUpdateRequest.endTime().toLocalTime());

        LocalDate meetingDate = defaultUpdateRequest.startTime().toLocalDate();
        LocalDateTime attendeeWorkEndAtDate = meetingDate.atTime(conflictAttendee.getWorkingEndTime());
        LocalDateTime meetingEndAfterAttendeeWorkEnd = attendeeWorkEndAtDate.plusHours(1);

        Location nonConflictLocation = new Location();
        nonConflictLocation.setId(mockLocation1.getId());
        nonConflictLocation.setWorkingEndTime(meetingEndAfterAttendeeWorkEnd.toLocalTime());

        UpdateMeetingRequestDTO requestWithTimeConflict = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                defaultUpdateRequest.startTime(),
                meetingEndAfterAttendeeWorkEnd,
                nonConflictLocation.getId(),
                Set.of(conflictAttendee.getId())
        );
        String expectedErrorMessage = String.format("Meeting end time (%s) is after attendee ID: %d working end time (%s).", meetingEndAfterAttendeeWorkEnd, conflictAttendee.getId(), attendeeWorkEndAtDate);

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(requestWithTimeConflict.locationId())).thenReturn(Optional.of(nonConflictLocation));
        when(attendeeRepository.findAllById(requestWithTimeConflict.attendeeIds())).thenReturn(List.of(conflictAttendee));
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, requestWithTimeConflict);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(requestWithTimeConflict.locationId());
        verify(attendeeRepository).findAllById(requestWithTimeConflict.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.startTime(), requestWithTimeConflict.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                requestWithTimeConflict.locationId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), requestWithTimeConflict.endTime(), requestWithTimeConflict.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }


    @Test
    void updateMeeting_shouldThrowIllegalArgumentException_whenLocationCapacityIsLessThanNumberOfAttendees() {
        Long meetingIdToUpdate = defaultMeeting.getId();

        Location locationWithLessCapacity = new Location();
        locationWithLessCapacity.setId(mockLocation1.getId());
        locationWithLessCapacity.setName(mockLocation1.getName());
        locationWithLessCapacity.setCapacity(1);

        List<Attendee> moreAttendees = List.of(mockAttendee1, mockAttendee2);
        Set<Long> moreAttendeeIds = Set.of(mockAttendee1.getId(), mockAttendee2.getId()); // Make sure that attendees in request > 1

        UpdateMeetingRequestDTO updateRequestWithLessCapacity = new UpdateMeetingRequestDTO(
                defaultUpdateRequest.title(),
                defaultUpdateRequest.startTime(),
                defaultUpdateRequest.endTime(),
                locationWithLessCapacity.getId(),
                moreAttendeeIds
        );

        String expectedErrorMessage = String.format("Meeting cannot be created. Location '%s' (ID: %d) has a capacity of %d, but %d attendees were invited.",
                locationWithLessCapacity.getName(),
                locationWithLessCapacity.getId(),
                locationWithLessCapacity.getCapacity(),
                updateRequestWithLessCapacity.attendeeIds().size());

        // Fetch data
        when(meetingRepository.findById(meetingIdToUpdate)).thenReturn(Optional.of(defaultMeeting));
        when(locationRepository.findById(updateRequestWithLessCapacity.locationId())).thenReturn(Optional.of(locationWithLessCapacity));
        when(attendeeRepository.findAllById(updateRequestWithLessCapacity.attendeeIds())).thenReturn(moreAttendees);
        // End fetch data
        // End fetch data
        when(meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                updateRequestWithLessCapacity.locationId(), updateRequestWithLessCapacity.startTime(), updateRequestWithLessCapacity.endTime())).thenReturn(List.of());
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                updateRequestWithLessCapacity.locationId(), updateRequestWithLessCapacity.endTime(), updateRequestWithLessCapacity.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), updateRequestWithLessCapacity.endTime(), updateRequestWithLessCapacity.startTime())).thenReturn(List.of());
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee2.getId(), updateRequestWithLessCapacity.endTime(), updateRequestWithLessCapacity.startTime())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.updateMeeting(meetingIdToUpdate, updateRequestWithLessCapacity);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).findById(meetingIdToUpdate);
        verify(locationRepository).findById(updateRequestWithLessCapacity.locationId());
        verify(attendeeRepository).findAllById(updateRequestWithLessCapacity.attendeeIds());
        verify(meetingRepository).findByLocation_idAndStartTimeAndEndTime(
                updateRequestWithLessCapacity.locationId(), updateRequestWithLessCapacity.startTime(), updateRequestWithLessCapacity.endTime());
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(
                updateRequestWithLessCapacity.locationId(), updateRequestWithLessCapacity.endTime(), updateRequestWithLessCapacity.startTime());
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(
                mockAttendee1.getId(), updateRequestWithLessCapacity.endTime(), updateRequestWithLessCapacity.startTime());
        verify(meetingRepository, never()).save(any(Meeting.class));
        verify(meetingMapper, never()).mapToMeetingDTO(any(Meeting.class));
    }

    // ==== END UPDATE ====

    // ==== DELETE ====

    @Test
    void deleteMeeting_shouldDeleteMeeting_whenMeetingExists() {
        Long meetingIdToDelete = DEFAULT_MEETING_ID;
        when(meetingRepository.existsById(meetingIdToDelete)).thenReturn(true);
        doNothing().when(meetingRepository).deleteById(meetingIdToDelete);

        assertDoesNotThrow(() -> meetingService.deleteMeeting(meetingIdToDelete));

        verify(meetingRepository).existsById(meetingIdToDelete);
        verify(meetingRepository).deleteById(meetingIdToDelete);
    }

    @Test
    void deleteMeeting_shouldThrowEntityNotFoundException_whenMeetingDoesNotExist() {
        Long nonExistentMeetingId = DEFAULT_MEETING_ID;
        String expectedErrorMessage = "Meeting not found with ID: " + nonExistentMeetingId;
        when(meetingRepository.existsById(nonExistentMeetingId)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> meetingService.deleteMeeting(nonExistentMeetingId)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(meetingRepository).existsById(nonExistentMeetingId);
        verify(meetingRepository, never()).deleteById(nonExistentMeetingId);
    }

    // ==== END DELETE ====

    // ==== AVAILABILITY ====

    // ...

    // ==== END AVAILABILITY ====

    // ==== SUGGESTIONS ====

    // ...

    // ==== END SUGGESTIONS ====
}
