package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.request.CommonAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.LocationAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.MeetingSuggestionRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.*;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AvailabilityServiceTest {
    private final LocalDate DEFAULT_DATE = LocalDate.of(Year.now().getValue() + 1, 8, 4);
    private final LocalDateTime DEFAULT_MEETING_START = DEFAULT_DATE.atTime(13, 0);
    private final LocalDateTime DEFAULT_MEETING_END = DEFAULT_DATE.atTime(14, 0);
    private final int DEFAULT_DURATION = 30;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AttendeeRepository attendeeRepository;
    @Mock
    private MeetingMapper meetingMapper;
    @Mock
    private LocationMapper locationMapper;
    @Spy

    @InjectMocks
    private AvailabilityService availabilityService;
    private Location mockLocation1, mockLocation2;
    private LocationDTO mockLocationDTO1, mockLocationDTO2;
    private Attendee mockAttendee1, mockAttendee2, mockAttendee3;
    private AttendeeDTO mockAttendeeDTO1, mockAttendeeDTO2, mockAttendeeDTO3;
    private Set<Attendee> mockAttendees;
    private Meeting mockMeeting1, mockMeeting2;
    private MeetingDTO mockMeetingDTO1, mockMeetingDTO2;
    private LocationAvailabilityRequestDTO defaultLocationAvailabilityRequest;
    private CommonAvailabilityRequestDTO defaultCommonAvailabilityRequest;

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
        mockLocation2.setCapacity(5);
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

        mockAttendee3 = new Attendee();
        mockAttendee3.setId(3L);
        mockAttendee3.setName("Attendee Three");
        mockAttendee3.setEmail("attendeethree@test.com");
        mockAttendee3.setWorkingStartTime(LocalTime.of(9, 0));
        mockAttendee3.setWorkingEndTime(LocalTime.of(17, 0));

        mockAttendees = Set.of(mockAttendee1, mockAttendee2, mockAttendee3);

        mockAttendeeDTO1 = new AttendeeDTO(mockAttendee1.getId(), mockAttendee1.getName(), mockAttendee1.getEmail());
        mockAttendeeDTO2 = new AttendeeDTO(mockAttendee2.getId(), mockAttendee2.getName(), mockAttendee2.getEmail());
        mockAttendeeDTO3 = new AttendeeDTO(mockAttendee3.getId(), mockAttendee3.getName(), mockAttendee3.getEmail());

        mockMeeting1 = new Meeting();
        mockMeeting1.setId(1L);
        mockMeeting1.setTitle("Meeting One");
        mockMeeting1.setStartTime(DEFAULT_MEETING_START);
        mockMeeting1.setEndTime(DEFAULT_MEETING_END);
        mockMeeting1.setLocation(mockLocation1);
        mockMeeting1.setAttendees(Set.of(mockAttendee1, mockAttendee2));

        mockMeeting2 = new Meeting();
        mockMeeting2.setId(2L);
        mockMeeting2.setTitle("Meeting Two");
        mockMeeting2.setStartTime(DEFAULT_MEETING_START.plusHours(2));
        mockMeeting2.setEndTime(DEFAULT_MEETING_END.plusHours(1).plusMinutes(30));
        mockMeeting2.setLocation(mockLocation2);
        mockMeeting2.setAttendees(Set.of(mockAttendee2, mockAttendee3));

        mockMeetingDTO1 = new MeetingDTO(
                mockMeeting1.getId(),
                mockMeeting1.getTitle(),
                mockMeeting1.getStartTime(),
                mockMeeting1.getEndTime(),
                mockLocationDTO1,
                Set.of(mockAttendeeDTO1, mockAttendeeDTO2));

        mockMeetingDTO2 = new MeetingDTO(
                mockMeeting2.getId(),
                mockMeeting2.getTitle(),
                mockMeeting2.getStartTime(),
                mockMeeting2.getEndTime(),
                mockLocationDTO2,
                Set.of(mockAttendeeDTO2, mockAttendeeDTO3));

        defaultLocationAvailabilityRequest = new LocationAvailabilityRequestDTO(
                DEFAULT_DATE,
                1,
                1
        );

        defaultCommonAvailabilityRequest = new CommonAvailabilityRequestDTO(
                mockAttendees.stream().map(Attendee::getId).collect(Collectors.toSet()),
                DEFAULT_DATE
        );
    }

    // getMeetingsForAttendeeInRange

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnListOfMeetingDTOs_whenMeetingsExistInRange() {
        Long attendeeId = mockAttendee1.getId();
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(DEFAULT_DATE);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(DEFAULT_DATE);

        List<Meeting> meetingsFromRepo = List.of(mockMeeting1, mockMeeting2);
        List<MeetingDTO> mappedMeetings = List.of(mockMeetingDTO1, mockMeetingDTO2);

        when(attendeeRepository.existsById(attendeeId)).thenReturn(true);
        when(meetingRepository.findByAttendees_idAndStartTimeBetween(attendeeId, rangeStart, rangeEnd)).thenReturn(meetingsFromRepo);
        when(meetingMapper.mapToMeetingDTOList(meetingsFromRepo)).thenReturn(mappedMeetings);

        List<MeetingDTO> results = availabilityService.getMeetingsForAttendeeInRange(attendeeId, rangeStart, rangeEnd);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(mockMeetingDTO1));
        assertTrue(results.contains(mockMeetingDTO2));

        verify(attendeeRepository).existsById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBetween(attendeeId, rangeStart, rangeEnd);
        verify(meetingMapper).mapToMeetingDTOList(meetingsFromRepo);
    }

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnEmptyList_whenNoMeetingsExistInRange() {
        Long attendeeId = mockAttendee1.getId();
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(DEFAULT_DATE);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(DEFAULT_DATE);

        List<Meeting> meetingsFromRepo = List.of();
        List<MeetingDTO> mappedMeetings = List.of();

        when(attendeeRepository.existsById(attendeeId)).thenReturn(true);
        when(meetingRepository.findByAttendees_idAndStartTimeBetween(attendeeId, rangeStart, rangeEnd)).thenReturn(meetingsFromRepo);
        when(meetingMapper.mapToMeetingDTOList(meetingsFromRepo)).thenReturn(mappedMeetings);

        List<MeetingDTO> results = availabilityService.getMeetingsForAttendeeInRange(attendeeId, rangeStart, rangeEnd);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(attendeeRepository).existsById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBetween(attendeeId, rangeStart, rangeEnd);
        verify(meetingMapper).mapToMeetingDTOList(meetingsFromRepo);
    }

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnEmptyList_whenAttendeeDoesNotExist() {
        Long nonExistentAttendeeId = mockAttendee1.getId();
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(DEFAULT_DATE);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(DEFAULT_DATE);

        when(attendeeRepository.existsById(nonExistentAttendeeId)).thenReturn(false);

        List<MeetingDTO> results = availabilityService.getMeetingsForAttendeeInRange(nonExistentAttendeeId, rangeStart, rangeEnd);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(attendeeRepository).existsById(nonExistentAttendeeId);
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingMapper, never()).mapToMeetingDTOList(anyList());
    }

    // getMeetingsForLocationInRange

    @Test
    void getMeetingsForLocationInRange_shouldReturnListOfMeetingDTOs_whenMeetingsExistInRange() {
        Long locationId = mockLocation1.getId();
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(DEFAULT_DATE);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(DEFAULT_DATE);

        List<Meeting> meetingsFromRepo = List.of(mockMeeting1, mockMeeting2);
        List<MeetingDTO> mappedMeetings = List.of(mockMeetingDTO1, mockMeetingDTO2);

        when(locationRepository.existsById(locationId)).thenReturn(true);
        when(meetingRepository.findByLocation_idAndStartTimeBetween(locationId, rangeStart, rangeEnd)).thenReturn(meetingsFromRepo);
        when(meetingMapper.mapToMeetingDTOList(meetingsFromRepo)).thenReturn(mappedMeetings);

        List<MeetingDTO> results = availabilityService.getMeetingsForLocationInRange(locationId, rangeStart, rangeEnd);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(mockMeetingDTO1));
        assertTrue(results.contains(mockMeetingDTO2));

        verify(locationRepository).existsById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBetween(locationId, rangeStart, rangeEnd);
        verify(meetingMapper).mapToMeetingDTOList(meetingsFromRepo);
    }

    @Test
    void getMeetingsForLocationInRange_shouldReturnEmptyList_whenNoMeetingsExistInRange() {
        Long locationId = mockLocation1.getId();
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(DEFAULT_DATE);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(DEFAULT_DATE);

        List<Meeting> meetingsFromRepo = List.of();
        List<MeetingDTO> mappedMeetings = List.of();

        when(locationRepository.existsById(locationId)).thenReturn(true);
        when(meetingRepository.findByLocation_idAndStartTimeBetween(locationId, rangeStart, rangeEnd)).thenReturn(meetingsFromRepo);
        when(meetingMapper.mapToMeetingDTOList(meetingsFromRepo)).thenReturn(mappedMeetings);

        List<MeetingDTO> results = availabilityService.getMeetingsForLocationInRange(locationId, rangeStart, rangeEnd);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(locationRepository).existsById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBetween(locationId, rangeStart, rangeEnd);
        verify(meetingMapper).mapToMeetingDTOList(meetingsFromRepo);
    }

    @Test
    void getMeetingsForAttendeeInRange_shouldReturnEmptyList_whenLocationDoesNotExist() {
        Long locationId = mockLocation1.getId();
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(DEFAULT_DATE);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(DEFAULT_DATE);

        when(locationRepository.existsById(locationId)).thenReturn(false);

        List<MeetingDTO> results = availabilityService.getMeetingsForLocationInRange(locationId, rangeStart, rangeEnd);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(locationRepository).existsById(locationId);
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(meetingMapper, never()).mapToMeetingDTOList(anyList());
    }

    // getAvailableTimeForLocation

    @Test
    void getAvailableTimeForLocation_shouldReturnFullSlot_whenNoMeetingsAndRangeWithinWorkingHours() {
        Long locationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockLocation1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockLocation1.getWorkingEndTime().atDate(date);
        AvailableSlotDTO fullDaySlot = new AvailableSlotDTO(rangeStart, rangeEnd);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(mockLocation1));
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart)).thenReturn(List.of());

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForLocation(locationId, date);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(fullDaySlot));

        verify(locationRepository).findById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForLocation_shouldReturnCorrectSlots_whenSingleMeetingExists() {
        Long locationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockLocation1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockLocation1.getWorkingEndTime().atDate(date);
        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(mockMeeting1);
        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, mockMeeting1.getStartTime());
        AvailableSlotDTO slot2 = new AvailableSlotDTO(mockMeeting1.getEndTime(), rangeEnd);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(mockLocation1));
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForLocation(locationId, date);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(slot1));
        assertTrue(results.contains(slot2));

        verify(locationRepository).findById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForLocation_shouldReturnCorrectSlots_whenMultipleMeetingsExist() {
        Long locationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockLocation1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockLocation1.getWorkingEndTime().atDate(date);
        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(mockMeeting1);
        meetingsFromRepo.add(mockMeeting2);
        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, mockMeeting1.getStartTime());
        AvailableSlotDTO slot2 = new AvailableSlotDTO(mockMeeting1.getEndTime(), mockMeeting2.getStartTime());
        AvailableSlotDTO slot3 = new AvailableSlotDTO(mockMeeting2.getEndTime(), rangeEnd);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(mockLocation1));
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForLocation(locationId, date);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.contains(slot1));
        assertTrue(results.contains(slot2));
        assertTrue(results.contains(slot3));

        verify(locationRepository).findById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForLocation_shouldReturnNoSlot_whenMeetingsAreBackToBackAndFillRange() {
        Long locationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockLocation1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockLocation1.getWorkingEndTime().atDate(date);

        Meeting meetingBackToBack = new Meeting();
        meetingBackToBack.setLocation(mockLocation1);
        meetingBackToBack.setStartTime(mockMeeting1.getEndTime());
        meetingBackToBack.setEndTime(mockMeeting1.getEndTime().plusMinutes(30));

        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(mockMeeting1);
        meetingsFromRepo.add(meetingBackToBack);
        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, mockMeeting1.getStartTime());
        AvailableSlotDTO slot2 = new AvailableSlotDTO(meetingBackToBack.getEndTime(), rangeEnd);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(mockLocation1));
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForLocation(locationId, date);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(slot1));
        assertTrue(results.contains(slot2));

        verify(locationRepository).findById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForLocation_shouldHandleMeetingAtStartOfEffectiveRange() {
        Long locationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockLocation1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockLocation1.getWorkingEndTime().atDate(date);

        Meeting meetingStartsAtRangeStart = new Meeting();
        meetingStartsAtRangeStart.setLocation(mockLocation1);
        meetingStartsAtRangeStart.setStartTime(rangeStart);
        meetingStartsAtRangeStart.setEndTime(rangeStart.plusHours(1));

        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(meetingStartsAtRangeStart);

        AvailableSlotDTO slot1 = new AvailableSlotDTO(meetingStartsAtRangeStart.getEndTime(), rangeEnd);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(mockLocation1));
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForLocation(locationId, date);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(slot1));

        verify(locationRepository).findById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForLocation_shouldHandleMeetingAtEndOfEffectiveRange() {
        Long locationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockLocation1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockLocation1.getWorkingEndTime().atDate(date);

        Meeting meetingEndAtRangeEnd = new Meeting();
        meetingEndAtRangeEnd.setLocation(mockLocation1);
        meetingEndAtRangeEnd.setStartTime(rangeEnd.minusHours(1));
        meetingEndAtRangeEnd.setEndTime(rangeEnd);

        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(meetingEndAtRangeEnd);

        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, meetingEndAtRangeEnd.getStartTime());

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(mockLocation1));
        when(meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForLocation(locationId, date);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(slot1));

        verify(locationRepository).findById(locationId);
        verify(meetingRepository).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForLocation_shouldThrowEntityNotFoundException_whenLocationDoesNotExist() {
        Long nonExistentLocationId = mockLocation1.getId();
        LocalDate date = DEFAULT_DATE;
        String expectedErrorMessage = "Location not found with ID: " + nonExistentLocationId;

        when(locationRepository.findById(nonExistentLocationId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getAvailableTimeForLocation(nonExistentLocationId, date);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findById(nonExistentLocationId);
        verify(meetingRepository, never()).findByLocation_idAndStartTimeBeforeAndEndTimeAfter(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // getAvailabilityForLocationsByDuration

    @Test
    void getAvailabilityForLocationsByDuration_shouldReturnFilteredLocationSlots_whenLocationsHaveAvailabilityMatchingCriteria() {
        int minCapacity = defaultLocationAvailabilityRequest.minimumCapacity();
        LocalDate date = defaultLocationAvailabilityRequest.date();
        List<Location> locationsFromRepo = List.of(mockLocation1, mockLocation2);

        when(locationRepository.findByCapacityGreaterThanEqual(minCapacity)).thenReturn(locationsFromRepo);

        AvailableSlotDTO slot1_loc1 = new AvailableSlotDTO(
                date.atTime(9, 0), date.atTime(10, 30)
        );
        AvailableSlotDTO slot2_loc1 = new AvailableSlotDTO(
                date.atTime(13, 0), date.atTime(14, 30)
        );
        AvailableSlotDTO slot1_loc2 = new AvailableSlotDTO(
                date.atTime(14, 0), date.atTime(15, 0)
        );

        doReturn(List.of(slot1_loc1, slot2_loc1)).when(availabilityService).getAvailableTimeForLocation(mockLocation1.getId(), date);
        doReturn(List.of(slot1_loc2)).when(availabilityService).getAvailableTimeForLocation(mockLocation2.getId(), date);

        when(locationMapper.mapToLocationDTO(mockLocation1)).thenReturn(mockLocationDTO1);
        when(locationMapper.mapToLocationDTO(mockLocation2)).thenReturn(mockLocationDTO2);

        LocationTimeSlotDTO expectedLocation1Slot1 = new LocationTimeSlotDTO(
                mockLocationDTO1, slot1_loc1
        );
        LocationTimeSlotDTO expectedLocation1Slot2 = new LocationTimeSlotDTO(
                mockLocationDTO1, slot2_loc1
        );
        LocationTimeSlotDTO expectedLocation2Slot = new LocationTimeSlotDTO(
                mockLocationDTO2, slot1_loc2
        );

        List<LocationTimeSlotDTO> expectedResults = List.of(expectedLocation1Slot1, expectedLocation1Slot2, expectedLocation2Slot);

        List<LocationTimeSlotDTO> results = availabilityService.getAvailabilityForLocationsByDuration(defaultLocationAvailabilityRequest);

        assertNotNull(results);
        assertEquals(3, results.size(), "Number of slots meeting duration criteria did not match.");
        assertTrue(results.containsAll(expectedResults) && expectedResults.containsAll(results), "Resulting slots do not match expected slots based on duration.");

        verify(locationRepository).findByCapacityGreaterThanEqual(minCapacity);
        verify(availabilityService).getAvailableTimeForLocation(mockLocation1.getId(), date);
        verify(availabilityService).getAvailableTimeForLocation(mockLocation2.getId(), date);
        verify(locationMapper).mapToLocationDTO(mockLocation1);
        verify(locationMapper).mapToLocationDTO(mockLocation2);
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldHandleEmptyLocationsList_whenMinCapacityNotProvided() {
        LocationAvailabilityRequestDTO requestWithNullCapacity = new LocationAvailabilityRequestDTO(
                defaultLocationAvailabilityRequest.date(),
                defaultLocationAvailabilityRequest.durationMinutes(),
                null
        );
        when(locationRepository.findAll()).thenReturn(List.of());

        List<LocationTimeSlotDTO> results = availabilityService.getAvailabilityForLocationsByDuration(requestWithNullCapacity);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(locationRepository).findAll();
        verify(locationRepository, never()).findByCapacityGreaterThanEqual(anyInt());
        verify(availabilityService, never()).getAvailableTimeForLocation(anyLong(), any(LocalDate.class));
        verify(locationMapper, never()).mapToLocationDTO(any(Location.class));
    }

    @Test
    void getAvailabilityForLocationsByDuration_throwsEntityNotFoundException_whenNoLocationMeetsCapacityCriteria() {
        Integer minCapacity = defaultLocationAvailabilityRequest.minimumCapacity();
        String expectedErrorMessage = "Locations not found with capacity equal or greater than: " + minCapacity;

        when(locationRepository.findByCapacityGreaterThanEqual(minCapacity)).thenReturn(List.of());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getAvailabilityForLocationsByDuration(defaultLocationAvailabilityRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(locationRepository).findByCapacityGreaterThanEqual(minCapacity);
        verify(availabilityService, never()).getAvailableTimeForLocation(anyLong(), any(LocalDate.class));
        verify(locationMapper, never()).mapToLocationDTO(any(Location.class));
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldCorrectlyFilterByMinCapacity() {
        int specificMinCapacity = 6;
        LocationAvailabilityRequestDTO requestWithGreaterCapacity = new LocationAvailabilityRequestDTO(
                defaultLocationAvailabilityRequest.date(),
                defaultLocationAvailabilityRequest.durationMinutes(),
                specificMinCapacity
        );
        LocalDate date = requestWithGreaterCapacity.date();
        List<Location> locationsFromRepo = List.of(mockLocation1);

        when(locationRepository.findByCapacityGreaterThanEqual(specificMinCapacity)).thenReturn(locationsFromRepo);

        AvailableSlotDTO slot1 = new AvailableSlotDTO(
                date.atTime(10, 0), date.atTime(12, 0)
        );

        doReturn(List.of(slot1)).when(availabilityService).getAvailableTimeForLocation(mockLocation1.getId(), date);

        when(locationMapper.mapToLocationDTO(mockLocation1)).thenReturn(mockLocationDTO1);

        LocationTimeSlotDTO expectedLocationSlot1 = new LocationTimeSlotDTO(mockLocationDTO1, slot1);
        List<LocationTimeSlotDTO> expectedResult = List.of(expectedLocationSlot1);

        List<LocationTimeSlotDTO> results = availabilityService.getAvailabilityForLocationsByDuration(requestWithGreaterCapacity);

        assertNotNull(results);
        assertEquals(1, results.size(), "Should only contain slots from locations meeting capacity criteria.");
        assertTrue(results.containsAll(expectedResult) && expectedResult.containsAll(results));

        verify(locationRepository).findByCapacityGreaterThanEqual(specificMinCapacity);
        verify(availabilityService).getAvailableTimeForLocation(mockLocation1.getId(), date);
        verify(availabilityService, never()).getAvailableTimeForLocation(mockLocation2.getId(), date);
        verify(locationMapper).mapToLocationDTO(mockLocation1);
        verify(locationMapper, never()).mapToLocationDTO(mockLocation2);
    }

    @Test
    void getAvailabilityForLocationsByDuration_shouldCorrectlyFilterByMinDuration() {
        LocalDate date = defaultCommonAvailabilityRequest.date();
        LocationAvailabilityRequestDTO requestWithGreaterDuration = new LocationAvailabilityRequestDTO(
                date,
                60,
                null
        );
        List<Location> locationsFromRepo = List.of(mockLocation1, mockLocation2);

        when(locationRepository.findAll()).thenReturn(locationsFromRepo);

        AvailableSlotDTO slot1_loc1_longEnough = new AvailableSlotDTO(
                date.atTime(9, 0), date.atTime(10, 30)
        );
        AvailableSlotDTO slot2_loc1_tooShort = new AvailableSlotDTO(
                date.atTime(11, 0), date.atTime(11, 30)
        );
        AvailableSlotDTO slot1_loc2_longEnough = new AvailableSlotDTO(
                date.atTime(14, 0), date.atTime(15, 0)
        );

        doReturn(List.of(slot1_loc1_longEnough, slot2_loc1_tooShort)).when(availabilityService).getAvailableTimeForLocation(mockLocation1.getId(), date);
        doReturn(List.of(slot1_loc2_longEnough)).when(availabilityService).getAvailableTimeForLocation(mockLocation2.getId(), date);

        when(locationMapper.mapToLocationDTO(mockLocation1)).thenReturn(mockLocationDTO1);
        when(locationMapper.mapToLocationDTO(mockLocation2)).thenReturn(mockLocationDTO2);

        LocationTimeSlotDTO expectedLocation1Slot = new LocationTimeSlotDTO(
                mockLocationDTO1, slot1_loc1_longEnough
        );
        LocationTimeSlotDTO expectedLocation2Slot = new LocationTimeSlotDTO(
                mockLocationDTO2, slot1_loc2_longEnough
        );
        List<LocationTimeSlotDTO> expectedResults = List.of(expectedLocation1Slot, expectedLocation2Slot);

        List<LocationTimeSlotDTO> results = availabilityService.getAvailabilityForLocationsByDuration(requestWithGreaterDuration);

        assertNotNull(results);
        assertEquals(2, results.size(), "Number of slots meeting duration criteria did not match.");
        assertTrue(results.containsAll(expectedResults) && expectedResults.containsAll(results), "Resulting slots do not match expected slots based on duration.");

        verify(locationRepository).findAll();
        verify(availabilityService).getAvailableTimeForLocation(mockLocation1.getId(), date);
        verify(availabilityService).getAvailableTimeForLocation(mockLocation2.getId(), date);
        verify(locationMapper).mapToLocationDTO(mockLocation1);
        verify(locationMapper).mapToLocationDTO(mockLocation2);
    }

    // getAvailableTimeForAttendee

    @Test
    void getAvailableTimeForAttendee_shouldReturnFullSlot_whenNoMeetingsAndRangeWithinWorkingHours() {
        Long attendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(date);
        AvailableSlotDTO fullDaySlot = new AvailableSlotDTO(rangeStart, rangeEnd);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(mockAttendee1));
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart)).thenReturn(List.of());

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForAttendee(attendeeId, date);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(fullDaySlot));

        verify(attendeeRepository).findById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForAttendee_shouldReturnCorrectSlots_whenSingleMeetingExists() {
        Long attendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(date);
        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(mockMeeting1);
        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, mockMeeting1.getStartTime());
        AvailableSlotDTO slot2 = new AvailableSlotDTO(mockMeeting1.getEndTime(), rangeEnd);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(mockAttendee1));
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForAttendee(attendeeId, date);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(slot1));
        assertTrue(results.contains(slot2));

        verify(attendeeRepository).findById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForAttendee_shouldReturnCorrectSlots_whenMultipleMeetingsExist() {
        Long attendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(date);
        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(mockMeeting1);
        meetingsFromRepo.add(mockMeeting2);
        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, mockMeeting1.getStartTime());
        AvailableSlotDTO slot2 = new AvailableSlotDTO(mockMeeting1.getEndTime(), mockMeeting2.getStartTime());
        AvailableSlotDTO slot3 = new AvailableSlotDTO(mockMeeting2.getEndTime(), rangeEnd);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(mockAttendee1));
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForAttendee(attendeeId, date);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.contains(slot1));
        assertTrue(results.contains(slot2));
        assertTrue(results.contains(slot3));

        verify(attendeeRepository).findById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForAttendee_shouldReturnNoSlot_whenMeetingsAreBackToBackAndFillRange() {
        Long attendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(date);

        Meeting meetingBackToBack = new Meeting();
        meetingBackToBack.setLocation(mockLocation1);
        meetingBackToBack.setStartTime(mockMeeting1.getEndTime());
        meetingBackToBack.setEndTime(mockMeeting1.getEndTime().plusMinutes(30));

        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(mockMeeting1);
        meetingsFromRepo.add(meetingBackToBack);
        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, mockMeeting1.getStartTime());
        AvailableSlotDTO slot2 = new AvailableSlotDTO(meetingBackToBack.getEndTime(), rangeEnd);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(mockAttendee1));
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForAttendee(attendeeId, date);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(slot1));
        assertTrue(results.contains(slot2));

        verify(attendeeRepository).findById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart);

    }

    @Test
    void getAvailableTimeForAttendee_shouldHandleMeetingAtStartOfEffectiveRange() {
        Long attendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(date);

        Meeting meetingStartsAtRangeStart = new Meeting();
        meetingStartsAtRangeStart.setLocation(mockLocation1);
        meetingStartsAtRangeStart.setStartTime(rangeStart);
        meetingStartsAtRangeStart.setEndTime(rangeStart.plusHours(1));

        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(meetingStartsAtRangeStart);

        AvailableSlotDTO slot1 = new AvailableSlotDTO(meetingStartsAtRangeStart.getEndTime(), rangeEnd);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(mockAttendee1));
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForAttendee(attendeeId, date);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(slot1));

        verify(attendeeRepository).findById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForAttendee_shouldHandleMeetingAtEndOfEffectiveRange() {
        Long attendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        LocalDateTime rangeStart = mockAttendee1.getWorkingStartTime().atDate(date);
        LocalDateTime rangeEnd = mockAttendee1.getWorkingEndTime().atDate(date);

        Meeting meetingEndAtRangeEnd = new Meeting();
        meetingEndAtRangeEnd.setLocation(mockLocation1);
        meetingEndAtRangeEnd.setStartTime(rangeEnd.minusHours(1));
        meetingEndAtRangeEnd.setEndTime(rangeEnd);

        List<Meeting> meetingsFromRepo = new ArrayList<>();
        meetingsFromRepo.add(meetingEndAtRangeEnd);

        AvailableSlotDTO slot1 = new AvailableSlotDTO(rangeStart, meetingEndAtRangeEnd.getStartTime());

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(mockAttendee1));
        when(meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart)).thenReturn(meetingsFromRepo);

        List<AvailableSlotDTO> results = availabilityService.getAvailableTimeForAttendee(attendeeId, date);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(slot1));

        verify(attendeeRepository).findById(attendeeId);
        verify(meetingRepository).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, rangeEnd, rangeStart);
    }

    @Test
    void getAvailableTimeForAttendee_shouldThrowEntityNotFoundException_whenAttendeeDoesNotExist() {
        Long nonExistentAttendeeId = mockAttendee1.getId();
        LocalDate date = DEFAULT_DATE;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(attendeeRepository.findById(nonExistentAttendeeId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            availabilityService.getAvailableTimeForAttendee(nonExistentAttendeeId, date);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(attendeeRepository).findById(nonExistentAttendeeId);
        verify(meetingRepository, never()).findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // getCommonAttendeeAvailability

    @Test
    void getCommonAttendeeAvailability_shouldReturnCommonSlots_whenAttendeesHaveOverlappingAvailability() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId(), mockAttendee2.getId());
        LocalDate date = defaultCommonAvailabilityRequest.date();

        CommonAvailabilityRequestDTO requestDTO = new CommonAvailabilityRequestDTO(
                attendeeIds,
                date
        );

        List<AvailableSlotDTO> attendee1Slots = List.of(
                slot("09:00", "11:00"),
                slot("14:00", "16:00")
        );
        List<AvailableSlotDTO> attendee2Slots = List.of(
                slot("10:00", "12:00"),
                slot("15:00", "17:00")
        );

        doReturn(attendee1Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        doReturn(attendee2Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);

        List<AvailableSlotDTO> expectedCommonSlots = List.of(
                slot("10:00", "11:00"),
                slot("15:00", "16:00")
        );

        List<AvailableSlotDTO> result = availabilityService.getCommonAttendeeAvailability(requestDTO);

        assertNotNull(result);
        assertEquals(expectedCommonSlots.size(), result.size());
        assertTrue(result.containsAll(expectedCommonSlots) && expectedCommonSlots.containsAll(result));

        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);
    }

    @Test
    void getCommonAttendeeAvailability_shouldReturnEmptyList_whenAttendeesHaveNoCommonAvailability() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId(), mockAttendee2.getId());
        LocalDate date = defaultCommonAvailabilityRequest.date();

        CommonAvailabilityRequestDTO requestDTO = new CommonAvailabilityRequestDTO(
                attendeeIds,
                date
        );

        List<AvailableSlotDTO> attendee1Slots = List.of(
                slot("09:00", "10:00")
        );
        List<AvailableSlotDTO> attendee2Slots = List.of(
                slot("11:00", "12:00")
        );

        doReturn(attendee1Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        doReturn(attendee2Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);

        List<AvailableSlotDTO> result = availabilityService.getCommonAttendeeAvailability(requestDTO);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);
    }

    @Test
    void getCommonAttendeeAvailability_shouldHandleEmptyAttendeeIdList() {
        LocalDate date = defaultCommonAvailabilityRequest.date();
        CommonAvailabilityRequestDTO requestWithEmptyIds = new CommonAvailabilityRequestDTO(
                Set.of(),
                date
        );

        List<AvailableSlotDTO> result = availabilityService.getCommonAttendeeAvailability(requestWithEmptyIds);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(availabilityService, never()).getAvailableTimeForAttendee(anyLong(), any(LocalDate.class));
    }

    @Test
    void getCommonAttendeeAvailability_shouldHandleSingleAttendeeIdInList() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = defaultCommonAvailabilityRequest.date();

        CommonAvailabilityRequestDTO requestWithSingleId = new CommonAvailabilityRequestDTO(
                attendeeIds,
                date
        );

        List<AvailableSlotDTO> attendee1Slots = List.of(
                slot("09:00", "12:00"),
                slot("14:00", "17:00")
        );

        doReturn(attendee1Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);

        List<AvailableSlotDTO> result = availabilityService.getCommonAttendeeAvailability(requestWithSingleId);

        assertNotNull(result);
        assertEquals(attendee1Slots.size(), result.size());
        assertTrue(result.containsAll(attendee1Slots) && attendee1Slots.containsAll(result));

        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
    }

    @Test
    void getCommonAttendeeAvailability_shouldCorrectlyIntersectMultipleAttendeeSchedules() {
        LocalDate date = defaultCommonAvailabilityRequest.date();

        List<AvailableSlotDTO> attendee1Slots = List.of(
                slot("09:00", "17:00")
        );
        List<AvailableSlotDTO> attendee2Slots = List.of(
                slot("09:00", "12:00"),
                slot("15:00", "17:00")
        );
        List<AvailableSlotDTO> attendee3Slots = List.of(
                slot("11:00", "16:00")
        );

        doReturn(attendee1Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        doReturn(attendee2Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);
        doReturn(attendee3Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee3.getId(), date);

        List<AvailableSlotDTO> expectedCommonSlots = List.of(
                slot("11:00", "12:00"),
                slot("15:00", "16:00")
        );

        List<AvailableSlotDTO> result = availabilityService.getCommonAttendeeAvailability(defaultCommonAvailabilityRequest);

        assertNotNull(result);
        assertEquals(expectedCommonSlots.size(), result.size());
        assertTrue(result.containsAll(expectedCommonSlots) && expectedCommonSlots.containsAll(result));

        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);
        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee3.getId(), date);
    }

    @Test
    void getCommonAttendeeAvailability_shouldStopEarlyIfCommonAvailabilityBecomesEmpty() {
        LocalDate date = defaultCommonAvailabilityRequest.date();

        List<AvailableSlotDTO> attendee1Slots = List.of(
                slot("09:00", "10:00")
        );
        List<AvailableSlotDTO> attendee2Slots = List.of(
                slot("11:00", "12:00")
        );
        List<AvailableSlotDTO> attendee3Slots = List.of(
                slot("09:00", "17:00")
        );

        doReturn(attendee1Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        doReturn(attendee2Slots).when(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);

        List<AvailableSlotDTO> result = availabilityService.getCommonAttendeeAvailability(defaultCommonAvailabilityRequest);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee1.getId(), date);
        verify(availabilityService).getAvailableTimeForAttendee(mockAttendee2.getId(), date);
        verify(availabilityService, never()).getAvailableTimeForAttendee(mockAttendee3.getId(), date);
    }

    // findMeetingSuggestions

    @Test
    void findMeetingSuggestions_shouldReturnSuggestions_whenCommonAvailabilityAndSuitableLocationsExist() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId(), mockAttendee2.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = 60;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(
                slot("10:00", "12:00"),
                slot("14:00", "15:30")
        );

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> suitableLocationSlots = List.of(
                new LocationTimeSlotDTO(mockLocationDTO1, slot("10:00", "11:00")),
                new LocationTimeSlotDTO(mockLocationDTO2, slot("11:00", "12:00")),
                new LocationTimeSlotDTO(mockLocationDTO1, slot("14:30", "15:30"))
        );

        doReturn(suitableLocationSlots).when(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);

        List<LocationTimeSlotDTO> expectedSuggestions = List.of(
                new LocationTimeSlotDTO(mockLocationDTO1, slot("10:00", "11:00")),
                new LocationTimeSlotDTO(mockLocationDTO2, slot("11:00", "12:00")),
                new LocationTimeSlotDTO(mockLocationDTO1, slot("14:30", "15:30"))
        );

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertNotNull(result);
        assertEquals(expectedSuggestions.size(), result.size());
        assertTrue(result.containsAll(expectedSuggestions) && expectedSuggestions.containsAll(result));

        verify(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);
        verify(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);
    }

    @Test
    void findMeetingSuggestions_shouldReturnEmptyList_whenNoCommonAttendeeAvailability() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);

        doReturn(List.of()).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);
        verify(availabilityService, never()).getAvailabilityForLocationsByDuration(any());
    }

    @Test
    void findMeetingSuggestions_shouldReturnEmptyList_whenCommonSlotsAreShorterThanDuration() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = 60;
        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);

        List<AvailableSlotDTO> shortCommonSlots = List.of(slot("10:00", "10:30"));

        doReturn(shortCommonSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);
        verify(availabilityService, never()).getAvailabilityForLocationsByDuration(any());
    }

    @Test
    void findMeetingSuggestions_shouldReturnEmptyList_whenNoLocationIsSuitable() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(slot("10:00", "11:00"));

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        doReturn(List.of()).when(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);
        verify(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);
    }

    @Test
    void findMeetingSuggestions_shouldHandleEmptyAttendeeListInRequest() {
        Set<Long> attendeeIds = Set.of();
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);

        doReturn(List.of()).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected no suggestions for empty attendee list");

        verify(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);
        verify(availabilityService, never()).getAvailabilityForLocationsByDuration(any());
    }

    @Test
    void findMeetingSuggestions_shouldHandleOneAttendeeAndOneLocation() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(slot("09:00", "10:00"));

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> locationSlots = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("09:15", "09:45")));

        doReturn(locationSlots).when(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);

        List<LocationTimeSlotDTO> expectedSuggestions = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("09:15", "09:45")));

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertNotNull(result);
        assertEquals(expectedSuggestions, result);
        assertTrue(expectedSuggestions.containsAll(result) && result.containsAll(expectedSuggestions));

        verify(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);
        verify(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);
    }

    @Test
    void findMeetingSuggestions_returnsFullOverlap_whenOverlapIsSufficient() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(slot("09:00", "11:00"));

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> locationSlots = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("10:00", "12:00")));

        doReturn(locationSlots).when(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);

        List<LocationTimeSlotDTO> expectedSuggestions = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("10:00", "11:00")));

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertEquals(expectedSuggestions, result);
    }

    @Test
    void findMeetingSuggestions_returnsFullOverlap_whenOverlapIsExactlyDuration() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(slot("09:00", "09:30"));

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> locationSlots = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("09:00", "09:30")));

        doReturn(locationSlots).when(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);

        List<LocationTimeSlotDTO> expectedSuggestions = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("09:00", "09:30")));

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertEquals(expectedSuggestions, result);
    }

    @Test
    void findMeetingSuggestions_returnsEmptyList_whenOverlapIsShorterThanDuration() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(slot("09:00", "09:20"));

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void findMeetingSuggestions_returnsEmptyList_whenNoActualOverlapExists() {
        Set<Long> attendeeIds = Set.of(mockAttendee1.getId());
        LocalDate date = DEFAULT_DATE;
        int durationMinutes = DEFAULT_DURATION;
        int requiredCapacity = attendeeIds.size();

        MeetingSuggestionRequestDTO request = new MeetingSuggestionRequestDTO(attendeeIds, durationMinutes, date);
        CommonAvailabilityRequestDTO commonAvailRequest = new CommonAvailabilityRequestDTO(attendeeIds, date);
        LocationAvailabilityRequestDTO locAvailRequest = new LocationAvailabilityRequestDTO(date, durationMinutes, requiredCapacity);

        List<AvailableSlotDTO> commonAttendeeSlots = List.of(slot("09:00", "10:00"));

        doReturn(commonAttendeeSlots).when(availabilityService).getCommonAttendeeAvailability(commonAvailRequest);

        List<LocationTimeSlotDTO> locationSlots = List.of(new LocationTimeSlotDTO(mockLocationDTO1, slot("11:00", "12:00")));

        doReturn(locationSlots).when(availabilityService).getAvailabilityForLocationsByDuration(locAvailRequest);

        List<LocationTimeSlotDTO> result = availabilityService.findMeetingSuggestions(request);

        assertTrue(result.isEmpty());
    }

    // === HELPER METHODS ===

    private AvailableSlotDTO slot(String startTimeStr, String endTimeStr) {
        return new AvailableSlotDTO(DEFAULT_DATE.atTime(LocalTime.parse(startTimeStr)), DEFAULT_DATE.atTime(LocalTime.parse(endTimeStr)));
    }

}
