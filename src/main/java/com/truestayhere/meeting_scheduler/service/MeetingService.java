package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.*;
import com.truestayhere.meeting_scheduler.exception.MeetingConflictException;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
import com.truestayhere.meeting_scheduler.mapper.MeetingMapper;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final LocationRepository locationRepository;
    private final AttendeeRepository attendeeRepository;
    private final MeetingMapper meetingMapper;
    private final LocationMapper locationMapper;

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    // Default working hours
    private static final LocalTime DEFAULT_WORKING_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORKING_END_TIME = LocalTime.of(17, 0);

    private record TimeWindow(LocalDateTime start, LocalDateTime end) {
    }


    // === CRUD METHODS ===


    /**
     * Creates a meeting based on provided data.
     *
     * @param requestDTO A CreateMeetingRequestDTO with the meeting data.
     * @return A MeetingDTO with the created meeting data.
     */
    @Transactional
    public MeetingDTO createMeeting(CreateMeetingRequestDTO requestDTO) {
        log.debug("Entering create meeting with title prefix: {}, locationId: {}, attendee count: {}",
                requestDTO.title().substring(0, Math.min(requestDTO.title().length(), 10)), // Logging title prefix only
                requestDTO.locationId(),
                requestDTO.attendeeIds() != null ? requestDTO.attendeeIds().size() : 0);

        // --- Duplicates Check ---

        checkMeetingDuplicates(requestDTO.locationId(), requestDTO.startTime(), requestDTO.endTime(), null);

        // --- Conflict/Overlap Check ---

        log.debug("Performing conflict checks before creating meeting.");
        checkLocationConflict(requestDTO.locationId(), requestDTO.startTime(), requestDTO.endTime(), null);
        checkAttendeeConflicts(requestDTO.attendeeIds(), requestDTO.startTime(), requestDTO.endTime(), null);
        log.debug("Conflict checks passed");

        // --- Fetch Location and Attendees Data ---

        Location location = findLocationEntityById(requestDTO.locationId());
        log.debug("Fetched location entity with ID: {}", location.getId());

        Set<Attendee> attendees = findAttendeesById(requestDTO.attendeeIds());
        log.debug("Fetched {} attendee entities", attendees.size());

        // --- Working Hours Check ---

        log.debug("Checking if meeting time is within location and attendees working hours.");
        checkMeetingWithinLocationWorkingHours(location, requestDTO.startTime(), requestDTO.endTime());
        checkMeetingWithinAttendeesWorkingHours(attendees, requestDTO.startTime(), requestDTO.endTime());
        log.debug("Working hours checks passed");

        // --- Capacity Check ---

        log.debug("Checking if meeting location capacity is enough for the number of attendees.");
        checkLocationCapacity(location, attendees.size());
        log.debug("Capacity check passed");

        // --- Creating Meeting ---

        Meeting newMeeting = new Meeting();
        newMeeting.setTitle(requestDTO.title());
        newMeeting.setStartTime(requestDTO.startTime());
        newMeeting.setEndTime(requestDTO.endTime());
        newMeeting.setLocation(location);
        newMeeting.setAttendees(attendees);

        // --- Save the Meeting ---

        log.debug("Attempting to save new meeting");

        Meeting savedMeeting = meetingRepository.save(newMeeting);

        log.info("Successfully created meeting with ID: {}", savedMeeting.getId());
        return meetingMapper.mapToMeetingDTO(savedMeeting);
    }


    /**
     * Fetches all meetings.
     *
     * @return A list of MeetingDTOs for all meetings.
     */
    public List<MeetingDTO> getAllMeetings() {
        List<Meeting> meetings = meetingRepository.findAll();
        return meetingMapper.mapToMeetingDTOList(meetings);
    }


    /**
     * Fetches a meeting based on provided ID.
     *
     * @param id The ID of the meeting.
     * @return A MeetingDTO for the found meeting.
     */
    public MeetingDTO getMeetingById(Long id) {
        Meeting foundMeeting = findMeetingEntityById(id);
        return meetingMapper.mapToMeetingDTO(foundMeeting);
    }


    /**
     * Updates a meeting based on provided ID and update data.
     *
     * @param id         The ID of the meeting to be updated.
     * @param requestDTO The UpdateMeetingRequestDTO with the updated data.
     * @return A MeetingDTO for the updated meeting.
     */
    @Transactional
    public MeetingDTO updateMeeting(Long id, UpdateMeetingRequestDTO requestDTO) {
        log.debug("Attempting to update meeting with ID: {}", id);

        Meeting existingMeeting = findMeetingEntityById(id);

        // --- Duplicates Check ---

        checkMeetingDuplicates(requestDTO.locationId(), requestDTO.startTime(), requestDTO.endTime(), id);

        // --- Conflict/Overlap Check ---

        log.debug("Performing conflict checks before updating meeting ID: {}", id);
        checkLocationConflict(requestDTO.locationId(), requestDTO.startTime(), requestDTO.endTime(), id);
        checkAttendeeConflicts(requestDTO.attendeeIds(), requestDTO.startTime(), requestDTO.endTime(), id);
        log.debug("Conflict checks passed for update");

        // --- Fetch Location and Attendees Data ---

        Location location = findLocationEntityById(requestDTO.locationId());
        log.debug("Fetched location entity with ID: {}", location.getId());

        Set<Attendee> attendees = findAttendeesById(requestDTO.attendeeIds());
        log.debug("Fetched {} attendee entities", attendees.size());

        // --- Working Hours Check ---

        log.debug("Checking if updated meeting time is within location and attendees working hours.");
        checkMeetingWithinLocationWorkingHours(location, requestDTO.startTime(), requestDTO.endTime());
        checkMeetingWithinAttendeesWorkingHours(attendees, requestDTO.startTime(), requestDTO.endTime());
        log.debug("Update working hours checks passed");

        // --- Capacity Check ---

        log.debug("Checking if updated meeting location capacity is enough for the number of attendees.");
        checkLocationCapacity(location, attendees.size());
        log.debug("Update capacity check passed");

        // --- Creating Meeting ---

        existingMeeting.setTitle(requestDTO.title());
        existingMeeting.setStartTime(requestDTO.startTime());
        existingMeeting.setEndTime(requestDTO.endTime());
        existingMeeting.setLocation(location);
        existingMeeting.setAttendees(attendees);

        // --- Save the Meeting ---

        log.debug("Attempting to save updated meeting");

        Meeting savedMeeting = meetingRepository.save(existingMeeting);

        log.info("Successfully updated meeting with ID: {}", savedMeeting.getId());
        return meetingMapper.mapToMeetingDTO(savedMeeting);
    }


    /**
     * Deletes a meeting by ID.
     *
     * @param id The ID of the meeting.
     */
    @Transactional
    public void deleteMeeting(Long id) {
        log.debug("Attempting to delete meeting with ID: {}", id);

        if (!meetingRepository.existsById(id)) {
            throw new EntityNotFoundException("Meeting not found with ID: " + id);
        }

        log.info("Successfully deleted meeting with ID: {}", id);
        meetingRepository.deleteById(id);
    }


    /**
     * Finds meetings for a specific attendee withing a given time range.
     *
     * @param attendeeId The ID of the attendee.
     * @param rangeStart The start of the time range (inclusive).
     * @param rangeEnd   The end of the time range (inclusive).
     * @return A list of MeetingDTOs for the attendee in that range.
     */
    public List<MeetingDTO> getMeetingsForAttendeeInRange(Long attendeeId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        log.debug("Fetching meetings for attendee ID: {} between {} and {}", attendeeId, rangeStart, rangeEnd);

        // Check if attendee exists
        if (!attendeeRepository.existsById(attendeeId)) {
            log.warn("Attempted to get schedule for non-existent attendee ID: {}", attendeeId);
            return List.of(); // Return an empty list
        }

        List<Meeting> meetings = meetingRepository.findByAttendees_idAndStartTimeBetween(attendeeId, rangeStart, rangeEnd);

        log.info("Found {} meetings for attendee ID: {} in the specified range.", meetings.size(), attendeeId);
        return meetingMapper.mapToMeetingDTOList(meetings);
    }


    /**
     * Finds meetings for a specific location within a given time range.
     *
     * @param locationId The ID of the location.
     * @param rangeStart The start of the time range (inclusive).
     * @param rangeEnd   The end of the time range (inclusive).
     * @return A list of MeetingDTOs for the location in that range.
     */
    public List<MeetingDTO> getMeetingsForLocationInRange(Long locationId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        log.debug("Fetching meetings for location ID: {} between {} and {}", locationId, rangeStart, rangeEnd);

        // Check if location exists
        if (!locationRepository.existsById(locationId)) {
            log.warn("Attempted to get schedule for non-existent location ID: {}", locationId);
            return List.of(); // Return an empty list
        }

        List<Meeting> meetings = meetingRepository.findByLocation_idAndStartTimeBetween(locationId, rangeStart, rangeEnd);

        log.info("Found {} meetings for location ID: {} in the specified range.", meetings.size(), locationId);
        return meetingMapper.mapToMeetingDTOList(meetings);
    }


    /**
     * Finds available time slots for a specific meeting within the working hours window on a specific day.
     *
     * @param id   The ID of the location.
     * @param date The date of the working day.
     * @return A list of AvailableSlotDTO with available time slots for the location on that date.
     */
    public List<AvailableSlotDTO> getAvailableTimeForLocation(Long id, LocalDate date) {
        log.debug("Finding available time slots for locationId: {} on date: {}", id, date);

        // fetch the location
        Location location = findLocationEntityById(id);

        // --- Handle Time Window Logic ---

        TimeWindow workingDayWindow = getWorkingDayWindow(location.getWorkingStartTime(), location.getWorkingEndTime(), date);
        log.debug("Working time window for location {}: {} to {}", id, workingDayWindow.start(), workingDayWindow.end());

        // --- End Time Window Logic Handling ---

        // Fetch meetings active in the working time window
        List<Meeting> existingMeetings = meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(id, workingDayWindow.end(), workingDayWindow.start());
        log.debug("Found {} booked meetings for locationId: {}, sorted by start time.", existingMeetings.size(), id);

        List<AvailableSlotDTO> availableSlots = findAvailableSlots(existingMeetings, workingDayWindow.start(), workingDayWindow.end());

        log.info("Calculated {} available time slots for locationId: {} on date: {}", availableSlots.size(), id, date);
        return availableSlots;
    }


    /**
     * Finds available time slots for a specific attendee within the working hours window on a specific day.
     *
     * @param id   The ID of the attendee.
     * @param date The date of the working day.
     * @return A list of AvailableSlotDTO with available time slots for the attendee on that date.
     */
    public List<AvailableSlotDTO> getAvailableTimeForAttendee(Long id, LocalDate date) {
        log.debug("Finding available time slots for attendeeId: {} on date: {}", id, date);

        // fetch the attendee
        Attendee attendee = findAttendeeEntityById(id);

        // --- Handle Time Window Logic ---

        TimeWindow workingDayWindow = getWorkingDayWindow(attendee.getWorkingStartTime(), attendee.getWorkingEndTime(), date);
        log.debug("Working time window for attendeeId {}: {} to {}", id, workingDayWindow.start(), workingDayWindow.end());

        // --- End Time Window Logic Handling ---

        // Fetch meetings active in the working time window
        List<Meeting> existingMeetings = meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(id, workingDayWindow.end(), workingDayWindow.start());
        log.debug("Found {} booked meeting for attendeeId: {}, sorted by start time.", existingMeetings.size(), id);

        List<AvailableSlotDTO> availableSlots = findAvailableSlots(existingMeetings, workingDayWindow.start(), workingDayWindow.end());

        log.info("Calculated {} available time slots for attendeeId: {} on date: {}", availableSlots.size(), id, date);
        return availableSlots;
    }


    /**
     * Finds suitable meeting time slots and locations based on attendee availability and required duration.
     * Refactored for better efficiency (Location-First Intersection).
     *
     * @param request DTO containing attendee IDs, desired duration, and date.
     * @return A list of potential meeting suggestions.
     */
    public List<MeetingSuggestionDTO> findMeetingSuggestions(MeetingSuggestionRequestDTO request) {
        log.info("Finding meeting suggestions for attendeeIds: {}, date: {}, duration: {} mins",
                request.attendeeIds(), request.date(), request.durationMinutes());

        // Fetch attendees by ID
        Set<Attendee> attendees = findAttendeesById(request.attendeeIds());
        if (attendees.isEmpty()) {
            log.warn("No attendees found for suggestion request.");
            return List.of();
        }

        // Calculate common availability slots for attendees
        List<AvailableSlotDTO> commonSlots = calculateAttendeeCommonAvailability(attendees, request.date());
        if (commonSlots.isEmpty()) {
            log.info("No common available time slots found for the specified attendees on {}", request.date());
            return List.of();
        }
        log.debug("Found {} common slots before duration filter.", commonSlots.size());

        // Filter common slots by duration
        List<AvailableSlotDTO> sufficientDurationGaps = filterSlotsByDuration(commonSlots, request.durationMinutes());
        if (sufficientDurationGaps.isEmpty()) {
            log.info("No common available time slots found with sufficient duration ({} mins).", request.durationMinutes());
            return List.of();
        }
        log.debug("Found {} common slots after duration filter.", sufficientDurationGaps.size());

        // Find locations that match capacity
        List<Location> suitableLocations = findLocationsByCapacityMin(attendees.size());
        log.debug("Found {} locations with capacity >= {}.", suitableLocations.size(), attendees.size());

        // Filter locations availability time slots for matching requirements
        List<MeetingSuggestionDTO> suggestions = generateSuggestions(sufficientDurationGaps, suitableLocations, request.durationMinutes(), request.date());
        log.info("Generated {} meeting suggestions.", suggestions.size());

        return suggestions;
    }


    // === HELPER METHODS ===


    // -- Fetch Methods ---


    // Accepts ID, returns Meeting Entity
    private Meeting findMeetingEntityById(Long id) {
        return meetingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Meeting not found with id: " + id));
    }


    // Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
    }


    // Accepts int, returns List<Location>
    private List<Location> findLocationsByCapacityMin(int minCapacity) {
        List<Location> locations = locationRepository.findByCapacityGreaterThanEqual(minCapacity);
        if (locations.isEmpty()) {
            throw new EntityNotFoundException("Locations not found with capacity equal or greater than: " + minCapacity);
        }
        return locations;
    }


    // Accepts ID, returns Attendee Entity
    private Attendee findAttendeeEntityById(Long id) {
        return attendeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendee not found with id: " + id));
    }


    // Accepts Set<ID>, returns Set<Attendee>
    private Set<Attendee> findAttendeesById(Set<Long> idSet) {
        Set<Attendee> attendees = new HashSet<>();
        if (idSet != null && !idSet.isEmpty()) {
            List<Attendee> foundAttendees = attendeeRepository.findAllById(idSet);
            if (foundAttendees.size() != idSet.size()) {
                // (Add later) show exactly which ones not found in the exception message
                throw new EntityNotFoundException("One or more attendees not found.");
            }
            attendees.addAll(foundAttendees);
        }
        return attendees;
    }


    // -- End of Fetch Methods ---


    // -- Validation Methods ---

    // Location Conflict Check (meeting overlap) - Accepts ID, LocalDateTime, throws MeetingConflictException
    private void checkLocationConflict(Long locationId, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToExclude) {
        log.debug("Checking location conflict for locationId: {}, startTime: {}, endTime: {}", locationId, startTime, endTime);

        // fetch conflicting meetings from the list
        List<Meeting> conflictMeetings = meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(locationId, endTime, startTime);

        // remove an excluded meeting from the list (for the meeting update scenario)
        if (meetingIdToExclude != null) {
            conflictMeetings = conflictMeetings.stream()
                    .filter(meeting -> !meeting.getId().equals(meetingIdToExclude))
                    .toList(); // returns an immutable list
        }

        // if conflicting meetings found - throw MeetingConflictException
        if (!conflictMeetings.isEmpty()) {
            String conflictMeetingIds = conflictMeetings.stream()
                    .map(m -> m.getId().toString())
                    .collect(Collectors.joining(", "));
            String errorMessage = String.format("Location conflict detected. Location ID %d is booked during the requested time by the meeting(s) with ID(s): %s", locationId, conflictMeetingIds);
            log.warn(errorMessage);

            throw new MeetingConflictException(errorMessage);
        }
        log.debug("No location conflict found for locationId: {}", locationId);
    }


    // Attendee conflict Check (meeting overlap) - Accepts ID, LocalDateTime, throws MeetingConflictException
    private void checkAttendeeConflicts(Set<Long> attendeeIds, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToExclude) {
        if (attendeeIds == null || attendeeIds.isEmpty()) {
            log.debug("No attendees provided, skipping attendee conflict check.");
            return;
        }

        log.debug("Checking attendee conflicts for attendeeIds: {}, startTime: {}, endTime: {}", attendeeIds, startTime, endTime);

        // for each attendee id in a list perform conflict check
        for (Long attendeeId : attendeeIds) {
            log.debug("Checking conflicts for attendeeId: {}", attendeeId);

            // fetch conflicting meetings from the database
            List<Meeting> conflictingMeetings = meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(attendeeId, endTime, startTime);

            // remove an excluded meeting from the list (for the meeting update scenario)
            if (meetingIdToExclude != null) {
                conflictingMeetings = conflictingMeetings.stream()
                        .filter(meeting -> !meeting.getId().equals(meetingIdToExclude))
                        .toList();
            }

            // if conflicting meetings found - throw MeetingConflictException
            if (!conflictingMeetings.isEmpty()) {
                String conflictingMeetingsIds = conflictingMeetings.stream()
                        .map(m -> m.getId().toString())
                        .collect(Collectors.joining(", "));
                String errorMessage = String.format("Attendee conflict detected. Attendee ID %d is already booked during the requested time by meeting(s) with ID(s): %s", attendeeId, conflictingMeetingsIds);

                log.warn(errorMessage);

                throw new MeetingConflictException(errorMessage);
            }
            log.debug("No conflicts found for attendeeId: {}", attendeeId);
        }
        log.debug("No attendee conflicts found for the provided list.");
    }


    // Meeting Duplicates Check - Accepts ID, LocalDateTime, throws IllegalArgumentException
    private void checkMeetingDuplicates(Long locationId, LocalDateTime startTime, LocalDateTime endTime, Long meetingIdToExclude) {

        log.debug("Checking duplicates for a meeting with locationId: {}, startTime: {}, endTime: {}", locationId, startTime, endTime);

        List<Meeting> duplicateMeetings = meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                locationId,
                startTime,
                endTime
        );

        // remove an excluded meeting from the list (for the meeting update scenario)
        if (meetingIdToExclude != null) {
            duplicateMeetings = duplicateMeetings.stream()
                    .filter(meeting -> !meeting.getId().equals(meetingIdToExclude))
                    .toList();
        }

        if (!duplicateMeetings.isEmpty()) {
            log.warn("Meeting conflict detected. Meeting with with locationId: {}, startTime: {}, endTime: {} already exists.", locationId, startTime, endTime);
            throw new IllegalArgumentException("Meeting with the same location, start time and end time already exists.");
        }

        log.debug("No duplicates found for the provided meeting.");
    }


    // Meeting time-window within Location's working hours Check - Accepts Location, LocalDateTime, trows IllegalArgumentException
    private void checkMeetingWithinLocationWorkingHours(Location location, LocalDateTime startTime, LocalDateTime endTime) {
        if (location == null) {
            return;
        }

        TimeWindow locationTimeWindow = getWorkingDayWindow(location.getWorkingStartTime(), location.getWorkingEndTime(), startTime.toLocalDate());

        if (startTime.isBefore(locationTimeWindow.start())) {
            String errorMessage = String.format("Meeting start time (%s) is before location's working start time (%s).", startTime, locationTimeWindow.start());
            log.warn("Location working hours violation: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        if (endTime.isAfter(locationTimeWindow.end())) {
            String errorMessage = String.format("Meeting end time (%s) is after location's working end time (%s).", endTime, locationTimeWindow.end());
            log.warn("Location working hours violation: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        log.debug("Meeting times are within location working hours.");
    }


    // Meeting time-window within attendees' working hours check - Accepts Attendee, LocalDateTime, throws IllegalArgumentException
    private void checkMeetingWithinAttendeesWorkingHours(Set<Attendee> attendees, LocalDateTime startTime, LocalDateTime endTime) {
        if (attendees == null || attendees.isEmpty()) {
            return;
        }

        for (Attendee attendee : attendees) {
            TimeWindow locationTimeWindow = getWorkingDayWindow(attendee.getWorkingStartTime(), attendee.getWorkingEndTime(), startTime.toLocalDate());

            if (startTime.isBefore(locationTimeWindow.start())) {
                String errorMessage = String.format("Meeting start time (%s) is before attendee ID: %d working start time (%s).", startTime, attendee.getId(), locationTimeWindow.start());
                log.warn("Attendee working hours violation: {}", errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            if (endTime.isAfter(locationTimeWindow.end())) {
                String errorMessage = String.format("Meeting end time (%s) is after attendee ID: %d working end time (%s).", endTime, attendee.getId(), locationTimeWindow.end());
                log.warn("Attendee working hours violation: {}", errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }
        log.debug("Meeting times are within attendees working hours.");
    }


    // Meeting location's capacity check - Accepts Location, Integer, throws IllegalArgumentException
    private void checkLocationCapacity(Location location, Integer requiredCapacity) {
        if (requiredCapacity == null) {
            return;
        }

        Integer locationCapacity = location.getCapacity();

        if (locationCapacity < requiredCapacity) {
            String errorMessage = String.format("Meeting cannot be created. Location '%s' (ID: %d) has a capacity of %d, but %d attendees were invited.",
                    location.getName(),
                    location.getId(),
                    locationCapacity,
                    requiredCapacity);
            log.warn("Capacity check failed: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        log.debug("Meeting attendees' count fits location's capacity.");
    }

    // -- End of Validation Methods ---

    // --- Availability Methods ---

    /**
     * Finds available time slots between booked meetings in a specified time window.
     *
     * @param meetings    The list of booked meetings.
     * @param windowStart The start of the working time window.
     * @param windowEnd   The end of the working time window.
     * @return A List of AvailableSlotDTO with the available time slots for the specified time window.
     */
    private List<AvailableSlotDTO> findAvailableSlots(List<Meeting> meetings, LocalDateTime windowStart, LocalDateTime windowEnd) {
        // All day is free if there are no meetings
        if (meetings == null || meetings.isEmpty()) {
            return List.of(new AvailableSlotDTO(windowStart, windowEnd));
        }

        // Sorting meetings by start time (in case it is not sorted)
        meetings.sort(Comparator.comparing(Meeting::getStartTime));

        List<AvailableSlotDTO> availableSlots = new ArrayList<>();

        // Set pointer at the start of working time-window
        LocalDateTime currentPointer = windowStart;

        // Calculating available time slots
        for (Meeting meeting : meetings) {
            // Not taking into account time before or after the time-window
            LocalDateTime meetingStart = maxTime(meeting.getStartTime(), windowStart);
            LocalDateTime meetingEnd = minTime(meeting.getEndTime(), windowEnd);

            log.trace("Processing booked meeting: ID {}, Window Start: {}, Window End: {}", meeting.getId(), meetingStart, meetingEnd);

            // If there is a gap between currentPointer and meeting start - add time slot
            if (meetingStart.isAfter(currentPointer)) {
                log.trace("Found available slot: {} to {}", currentPointer, meetingStart);
                availableSlots.add(new AvailableSlotDTO(currentPointer, meetingStart));
            }

            // Move a pointer to the end of the meeting (or not move in case meeting overlap)
            currentPointer = maxTime(currentPointer, meetingEnd);
            log.trace("Pointer moved to: {}", currentPointer);
        }

        // In case there is a time between last meeting end and working time window end
        if (windowEnd.isAfter(currentPointer)) {
            log.trace("Found final available slot: {} to {}", currentPointer, windowEnd);
            availableSlots.add(new AvailableSlotDTO(currentPointer, windowEnd));
        }

        log.info("Calculated {} available time slots between {} and {}", availableSlots.size(), windowStart, windowEnd);
        return availableSlots;
    }


    /**
     * Calculates the time slots where ALL provided attendees are available on a given date.
     *
     * @param attendees The list of the attendees.
     * @param date      The date to calculate time slots.
     * @return A list of AvailableSlotDTOs with common available time slots for the provided attendees.
     */
    private List<AvailableSlotDTO> calculateAttendeeCommonAvailability(
            Set<Attendee> attendees, LocalDate date) {

        // If there are no attendees provided stop calculation
        if (attendees.isEmpty()) {
            log.warn("Attendee set is null or empty for common availability calculation.");
            return Collections.emptyList();
        }

        // Initialize iterator
        Iterator<Attendee> iterator = attendees.iterator();
        Attendee firstAttendee = iterator.next();

        // Assign the initial availability to the first attendee's available time slots
        List<AvailableSlotDTO> commonAvailability = getAvailableTimeForAttendee(firstAttendee.getId(), date);
        log.debug("Initial availability for attendee {}: {}", firstAttendee.getId(), commonAvailability.size());

        // Iterate through the attendee list
        while (iterator.hasNext()) {
            // If common availability became empty stop calculation
            if (commonAvailability.isEmpty()) {
                log.debug("Common availability became empty, stopping intersection early.");
                break;
            }

            // Get attendee availability
            Attendee currentAttendee = iterator.next();
            List<AvailableSlotDTO> attendeeAvailability = getAvailableTimeForAttendee(currentAttendee.getId(), date);
            log.debug("Availability for attendee {}: {}", currentAttendee.getId(), attendeeAvailability.size());

            // Intersect availability time slots with common time slots
            commonAvailability = intersectAvailability(commonAvailability, attendeeAvailability);
            log.debug("Common availability after intersecting with attendee {}: {}", currentAttendee.getId(), commonAvailability.size());
        }

        // Return a common available slots list
        log.info("Final common availability slots count: {}", commonAvailability.size());
        return commonAvailability;
    }


    // Finds intersecting time slots between two AvailableSlotDTO lists, Accepts List<AvailableSlotDTO>, returns List<AvailableSlotDTO> intersected slot list.
    private List<AvailableSlotDTO> intersectAvailability(List<AvailableSlotDTO> list1, List<AvailableSlotDTO> list2) {
        log.debug("Intersecting availability lists. List1 size: {}, List2 size: {}", list1.size(), list2.size());
        if (list1.isEmpty() || list2.isEmpty()) {
            log.debug("One or both lists are empty, returning empty intersection.");
            return Collections.emptyList();
        }

        List<AvailableSlotDTO> intersection = new ArrayList<>();
        int i = 0, j = 0;

        while (i < list1.size() && j < list2.size()) {
            AvailableSlotDTO slot1 = list1.get(i);
            AvailableSlotDTO slot2 = list2.get(j);
            log.trace("Comparing Slot1[{}]: {} - {} with Slot2[{}]: {} - {}", i, slot1.startTime(), slot1.endTime(), j, slot2.startTime(), slot2.endTime());

            LocalDateTime overlapStart = maxTime(slot1.startTime(), slot2.startTime());
            LocalDateTime overlapEnd = minTime(slot1.endTime(), slot2.endTime());
            log.trace("... Calculated potential overlap: {} - {}", overlapStart, overlapEnd);

            if (overlapStart.isBefore(overlapEnd)) {
                log.trace("... Valid overlap found: {} - {}. Adding to intersection.", overlapStart, overlapEnd);
                intersection.add(new AvailableSlotDTO(overlapStart, overlapEnd));
            } else {
                log.trace("... No valid overlap (start not before end).");
            }

            if (slot1.endTime().isBefore(slot2.endTime())) {
                log.trace("... Advancing pointer i (Slot1 ends earlier). Old i={}, New i={}", i, i + 1);
                i++;
            } else if (slot2.endTime().isBefore(slot1.endTime())) {
                log.trace("... Advancing pointer j (Slot2 ends earlier). Old j={}, New j={}", j, j + 1);
                j++;
            } else {
                log.trace("... Both slots end simultaneously. Advancing pointers i and j. Old i={}, j={}, New i={}, j={}", i, j, i + 1, j + 1);
                i++;
                j++;
            }
        }
        log.debug("Raw intersection found {} slots before merging.", intersection.size());

        List<AvailableSlotDTO> mergedIntersection = mergeOverlappingSlots(intersection);
        log.debug("Intersection complete. Returning {} merged slots.", mergedIntersection.size());

        return mergedIntersection;
    }


    // Merges overlapping time slots in a list, accepts List<AvailableSlotDTO>, returns List<AvailableSlotDTO> merged slots list.
    private List<AvailableSlotDTO> mergeOverlappingSlots(List<AvailableSlotDTO> slots) {
        log.debug("Merging {} input slots.", slots.size());
        if (slots.size() <= 1) {
            log.debug("List has 0 or 1 slot, no merging needed.");
            return slots == null ? Collections.emptyList() : slots;
        }

        slots.sort(Comparator.comparing(AvailableSlotDTO::startTime));
        log.trace("Slots sorted by start time.");

        LinkedList<AvailableSlotDTO> merged = new LinkedList<>();
        merged.add(slots.get(0));
        log.trace("Initialized merged list with first slot: {} - {}", slots.get(0).startTime(), slots.get(0).endTime());

        for (int i = 1; i < slots.size(); i++) {
            AvailableSlotDTO current = slots.get(i);
            AvailableSlotDTO lastMerged = merged.getLast();

            log.trace("Processing slot {}: {} - {}. Comparing with last merged: {} - {}", i, current.startTime(), current.endTime(), lastMerged.startTime(), lastMerged.endTime());

            if (!current.startTime().isAfter(lastMerged.endTime())) {
                log.trace("... Overlap/adjacency detected.");

                LocalDateTime newEndTime = maxTime(lastMerged.endTime(), current.endTime());

                if (newEndTime.isAfter(lastMerged.endTime())) {
                    log.trace("... Merging. Updating last merged end time from {} to {}", lastMerged.endTime(), newEndTime);
                    merged.set(merged.size() - 1, new AvailableSlotDTO(lastMerged.startTime(), newEndTime));
                } else {
                    log.trace("... Current slot is fully contained within last merged, no change needed.");
                }
            } else {
                log.trace("... No overlap. Adding current slot as a new entry.");
                merged.add(current);
            }
        }

        log.debug("Merging complete. Resulting list size: {}", merged.size());
        return merged;
    }


    // Returns List<AvailableSlotDTO> filtered by duration equal or greater to provided duration
    private List<AvailableSlotDTO> filterSlotsByDuration(List<AvailableSlotDTO> slots, int durationMinutes) {
        return slots.stream()
                .filter(slot -> Duration.between(slot.startTime(), slot.endTime()).toMinutes() >= durationMinutes)
                .collect(Collectors.toList());
    }


    /**
     * Generate potential meeting time windows for a specific date based on available time gaps, location and meeting duration.
     *
     * @param gaps            List of attendees' available time gaps.
     * @param locations       List of suitable locations.
     * @param durationMinutes The meeting minimal duration.
     * @param date            The date of the meeting.
     * @return List of MeetingSuggestionDTO with an effective time window start, end and available location.
     */
    private List<MeetingSuggestionDTO> generateSuggestions(
            List<AvailableSlotDTO> gaps, List<Location> locations, int durationMinutes, LocalDate date) {
        List<MeetingSuggestionDTO> suggestions = new ArrayList<>();
        Duration duration = Duration.ofMinutes(durationMinutes);
        log.debug("Generating suggestions. Common Gaps: {}, Suitable Locations: {}", gaps.size(), locations.size());

        for (Location location : locations) {
            log.debug("Processing location: {} (ID: {})", location.getName(), location.getId());

            // Get available slots for location
            List<AvailableSlotDTO> locationAvailability = getAvailableTimeForLocation(location.getId(), date);
            log.trace("... Location {} has {} slots", location.getName(), locationAvailability.size());

            // Find intersection between available attendee slots and location's slots
            List<AvailableSlotDTO> intersectionSlots = intersectAvailability(gaps, locationAvailability);
            if (intersectionSlots.isEmpty()) {
                log.debug("... No intersection found between attendee and location {} availability.", location.getName());
                continue;
            }
            log.trace("... Found {} intersection slots with location {}.", intersectionSlots.size(), location.getName());

            // Filter slots by duration
            List<AvailableSlotDTO> sufficientSlots = filterSlotsByDuration(intersectionSlots, durationMinutes);
            if (sufficientSlots.isEmpty()) {
                log.debug("... No sufficient slots found for location {} with duration >= {} mins.", location.getName(), durationMinutes);
                continue;
            }

            // Add suggestions to the list
            LocationDTO locationDTO = locationMapper.mapToLocationDTO(location);
            for (AvailableSlotDTO slot : sufficientSlots) {
                suggestions.add(new MeetingSuggestionDTO(slot.startTime(), slot.endTime(), locationDTO));
            }
        }

        // Filter results by start time and location name
        suggestions.sort(Comparator.comparing(MeetingSuggestionDTO::startTime).thenComparing(s -> s.locationDTO().name()));

        log.info("Generated {} meeting suggestions.", suggestions.size());
        return suggestions;
    }


    /**
     * Checks if a location has AT LEAST ONE available time block of a minimum duration
     * that falls within the requested check window [startTime, endTime).
     *
     * @param locationId      The ID of the location to check.
     * @param startTime       The start of the window to look within.
     * @param endTime         The end of the window to look within.
     * @param durationMinutes The minimum required duration of the free block.
     * @return true if at least one suitable free block exists, false otherwise.
     */
    private boolean isLocationAvailableForSlot(Long locationId, LocalDateTime startTime, LocalDateTime endTime, int durationMinutes) {
        // Fetch available time slots for the location
        List<AvailableSlotDTO> locationSlots = getAvailableTimeForLocation(locationId, startTime.toLocalDate());

        log.trace("Checking location ID: {} for duration {} within {} - {}. All Location Slots: {}",
                locationId, durationMinutes, startTime, endTime, locationSlots);

        for (AvailableSlotDTO locSlot : locationSlots) {
            LocalDateTime overlapStart = maxTime(locSlot.startTime(), startTime);
            LocalDateTime overlapEnd = minTime(locSlot.endTime(), endTime);

            long overlapDuration = Duration.between(overlapStart, overlapEnd).toMinutes();

            if (overlapStart.isBefore(overlapEnd)) {
                if (overlapDuration >= durationMinutes) {
                    log.trace("---> Found suitable overlap: {} - {} (Duration: {} mins) within Loc Slot: {}",
                            overlapStart, overlapEnd, overlapDuration, locSlot);
                }
                return true;
            } else {
                log.trace("---> Overlap {} - {} (Duration: {} mins) is too short.",
                        overlapStart, overlapEnd, overlapDuration);
            }
            log.trace("---> No suitable slot of {} mins found for loc {} within {} - {}",
                    durationMinutes, locationId, startTime, endTime);
        }

        return false;
    }

    // --- End of Availability Methods ---


    // --- Time Management Methods ---


    /**
     * Get an effective window within working hours for a meeting gap.
     *
     * @param generalGap The meeting gap.
     * @param startTime  The working start time.
     * @param endTime    The working end time.
     * @param date       The date the working day.
     * @return Optional<AvailableSlotDTO> with the effective window start and end time or null.
     */
    private Optional<AvailableSlotDTO> calculateEffectiveWindow(
            AvailableSlotDTO generalGap, LocalTime startTime, LocalTime endTime, LocalDate date) {

        TimeWindow locationWindow = getWorkingDayWindow(startTime, endTime, date);

        LocalDateTime effectiveStart = maxTime(generalGap.startTime(), locationWindow.start());
        LocalDateTime effectiveEnd = minTime(generalGap.endTime(), locationWindow.end());

        if (effectiveStart.isBefore(effectiveEnd)) {
            return Optional.of(new AvailableSlotDTO(effectiveStart, effectiveEnd));
        } else {
            log.trace("-- No effective window overlap between attendee gap {} and location window {}",
                    generalGap, locationWindow);
            return Optional.empty();
        }
    }


    /**
     * Calculate working shift time for the specified date.
     *
     * @param startTime The working day start time.
     * @param endTime   The working day end time.
     * @param date      The date to be assigned.
     * @return TimeWindow record with LocalDateTime working shift start and end time with an assigned date.
     */
    private TimeWindow getWorkingDayWindow(LocalTime startTime, LocalTime endTime, LocalDate date) {
        LocalTime workStart = startTime != null ?
                startTime : DEFAULT_WORKING_START_TIME;
        LocalTime workEnd = endTime != null ?
                endTime : DEFAULT_WORKING_END_TIME;

        LocalDateTime windowStart;
        LocalDateTime windowEnd;

        // Calculate night shift
        if (workEnd.isBefore(workStart)) {
            windowStart = date.atTime(workStart);
            windowEnd = date.plusDays(1).atTime(workEnd);
            log.trace("Calculated overnight working window: {} - {}",
                    windowStart, windowEnd);
        } else {
            windowStart = date.atTime(workStart);
            windowEnd = date.atTime(workEnd);
            log.trace("Calculated same-day working window: {} - {}",
                    windowStart, windowEnd);
        }

        return new TimeWindow(windowStart, windowEnd);
    }


    /**
     * Returns LocalDateTime object set after another one.
     *
     * @param d1 The first LocalDateTime.
     * @param d2 The second LocalDateTime.
     * @return The LocalDateTime that is later.
     */
    private LocalDateTime maxTime(LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null) return d2;
        if (d2 == null) return d1;

        return d1.isAfter(d2) ? d1 : d2;
    }

    /**
     * Returns LocalDateTime object set before another one.
     *
     * @param d1 The first LocalDateTime.
     * @param d2 The second LocalDateTime.
     * @return The LocalDateTime that is earlier.
     */
    private LocalDateTime minTime(LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null) return d2;
        if (d2 == null) return d1;

        return d1.isBefore(d2) ? d1 : d2;
    }

    // --- End of Time Management Methods ---
}
