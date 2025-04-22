package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.*;
import com.truestayhere.meeting_scheduler.exception.MeetingConflictException;
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

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    // Default working hours
    private static final LocalTime DEFAULT_WORKING_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORKING_END_TIME = LocalTime.of(17, 0);


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

        // --- Creating Meeting ---

        log.debug("Attempting to save new meeting");

        Meeting newMeeting = new Meeting();
        newMeeting.setTitle(requestDTO.title());
        newMeeting.setStartTime(requestDTO.startTime());
        newMeeting.setEndTime(requestDTO.endTime());
        newMeeting.setLocation(location);
        newMeeting.setAttendees(attendees);

        // --- Working Hours Check ---

        log.debug("Checking if meeting time is within location and attendees working hours.");
        checkMeetingWithinLocationWorkingHours(newMeeting);
        checkMeetingWithinAttendeesWorkingHours(newMeeting);
        log.debug("Working hours checks passed");

        // --- Save the Meeting ---

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

        // --- Creating Meeting ---

        existingMeeting.setTitle(requestDTO.title());
        existingMeeting.setStartTime(requestDTO.startTime());
        existingMeeting.setEndTime(requestDTO.endTime());
        existingMeeting.setLocation(location);
        existingMeeting.setAttendees(attendees);

        // --- Working Hours Check ---

        log.debug("Checking if updated meeting time is within location and attendees working hours.");
        checkMeetingWithinLocationWorkingHours(existingMeeting);
        checkMeetingWithinAttendeesWorkingHours(existingMeeting);
        log.debug("Update working hours checks passed");

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

        // Get working hours from location or default values
        LocalTime startOfDay = location.getWorkingStartTime() != null ?
                location.getWorkingStartTime() : DEFAULT_WORKING_START_TIME;
        LocalTime endOfDay = location.getWorkingEndTime() != null ?
                location.getWorkingEndTime() : DEFAULT_WORKING_END_TIME;

        // --- Handle Time Window Logic ---

        LocalDateTime windowStart;
        LocalDateTime windowEnd;

        if (endOfDay.isBefore(startOfDay)) {
            windowStart = date.atTime(startOfDay);
            windowEnd = date.plusDays(1).atTime(endOfDay);
            log.debug("Overnight shift detected for location {}.", id);
        } else {
            windowStart = date.atTime(startOfDay);
            windowEnd = date.atTime(endOfDay);
        }

        // --- End Time Window Logic Handling ---

        log.debug("Working time window for location {}: {} to {}", id, windowStart, windowEnd);

        // Fetch meetings active in the working time window
        List<Meeting> existingMeetings = meetingRepository.findByLocation_idAndStartTimeBeforeAndEndTimeAfter(id, windowEnd, windowStart);
        log.debug("Found {} booked meetings for locationId: {}, sorted by start time.", id, existingMeetings.size());

        List<AvailableSlotDTO> availableSlots = findAvailableSlots(existingMeetings, windowStart, windowEnd);

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

        // Get working hours from location or default values
        LocalTime startOfDay = attendee.getWorkingStartTime() != null ?
                attendee.getWorkingStartTime() : DEFAULT_WORKING_START_TIME;
        LocalTime endOfDay = attendee.getWorkingEndTime() != null ?
                attendee.getWorkingEndTime() : DEFAULT_WORKING_END_TIME;

        // --- Handle Time Window Logic ---

        LocalDateTime windowStart;
        LocalDateTime windowEnd;

        if (endOfDay.isBefore(startOfDay)) {
            windowStart = date.atTime(startOfDay);
            windowEnd = date.plusDays(1).atTime(endOfDay);
            log.debug("Overnight shift detected for attendee {}.", id);
        } else {
            windowStart = date.atTime(startOfDay);
            windowEnd = date.atTime(endOfDay);
        }

        // --- End Time Window Logic Handling ---

        log.debug("Working time window for attendee {}: {} to {}", id, windowStart, windowEnd);

        // Fetch meetings active in the working time window
        List<Meeting> existingMeetings = meetingRepository.findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(id, windowEnd, windowStart);
        log.debug("Found {} booked meeting for attendeeId: {}, sorted by start time.", id, existingMeetings.size());

        List<AvailableSlotDTO> availableSlots = findAvailableSlots(existingMeetings, windowStart, windowEnd);

        log.info("Calculated {} available time slots for attendeeId: {} on date: {}", availableSlots.size(), id, date);
        return availableSlots;
    }


    // === HELPER METHODS ===


    // Accepts ID, returns Meeting Entity
    private Meeting findMeetingEntityById(Long id) {
        return meetingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Meeting not found with id: " + id));
    }


    // Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
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


    // Meeting time-window within Location's working hours Check - Accepts Meeting, throws IllegalArgumentException
    private void checkMeetingWithinLocationWorkingHours(Meeting meeting) {
        Location location = meeting.getLocation();

        if (location == null) {
            return;
        }

        LocalTime locationStart = location.getWorkingStartTime() != null ?
                location.getWorkingStartTime() : DEFAULT_WORKING_START_TIME;
        LocalTime locationEnd = location.getWorkingEndTime() != null ?
                location.getWorkingEndTime() : DEFAULT_WORKING_END_TIME;

        LocalTime meetingStartTimeOfDay = meeting.getStartTime().toLocalTime();
        LocalTime meetingEndTimeOfDay = meeting.getEndTime().toLocalTime();

        if (meetingStartTimeOfDay.isBefore(locationStart)) {
            String errorMessage = String.format("Meeting start time (%s) is before location's working start time (%s).", meetingStartTimeOfDay, locationStart);
            log.warn("Location working hours violation: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        if (meetingEndTimeOfDay.isAfter(locationEnd)) {
            String errorMessage = String.format("Meeting end time (%s) is after location's working end time (%s).", meetingEndTimeOfDay, locationEnd);
            log.warn("Location working hours violation: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        log.debug("Meeting times are within location working hours.");
    }


    // Meeting time-window within attendees' working hours check - Accepts Meeting, throws IllegalArgumentException
    private void checkMeetingWithinAttendeesWorkingHours(Meeting meeting) {
        Set<Attendee> attendees = meeting.getAttendees();
        if (attendees == null || attendees.isEmpty()) {
            return;
        }

        LocalTime meetingStartTimeOfDay = meeting.getStartTime().toLocalTime();
        LocalTime meetingEndTimeOfDay = meeting.getEndTime().toLocalTime();

        for (Attendee attendee : attendees) {
            LocalTime attendeeStart = attendee.getWorkingStartTime() != null ?
                    attendee.getWorkingStartTime() : DEFAULT_WORKING_START_TIME;
            LocalTime attendeeEnd = attendee.getWorkingEndTime() != null ?
                    attendee.getWorkingEndTime() : DEFAULT_WORKING_END_TIME;

            if (meetingStartTimeOfDay.isBefore(attendeeStart)) {
                String errorMessage = String.format("Meeting start time (%s) is before attendee ID: %d working start time (%s).", meetingStartTimeOfDay, attendee.getId(), attendeeStart);
                log.warn("Attendee working hours violation: {}", errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            if (meetingEndTimeOfDay.isAfter(attendeeEnd)) {
                String errorMessage = String.format("Meeting end time (%s) is after attendee ID: %d working end time (%s).", meetingEndTimeOfDay, attendee.getId(), attendeeEnd);
                log.warn("Attendee working hours violation: {}", errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }
        log.debug("Meeting times are within attendees working hours.");
    }


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
            LocalDateTime meetingStart = max(meeting.getStartTime(), windowStart);
            LocalDateTime meetingEnd = min(meeting.getEndTime(), windowEnd);

            log.trace("Processing booked meeting: ID {}, Window Start: {}, Window End: {}", meeting.getId(), meetingStart, meetingEnd);

            // If there is a gap between currentPointer and meeting start - add time slot
            if (meetingStart.isAfter(currentPointer)) {
                log.trace("Found available slot: {} to {}", currentPointer, meetingStart);
                availableSlots.add(new AvailableSlotDTO(currentPointer, meetingStart));
            }

            // Move a pointer to the end of the meeting (or not move in case meeting overlap)
            currentPointer = max(currentPointer, meetingEnd);
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
     * Returns LocalDateTime object set after another one.
     *
     * @param d1 The first LocalDateTime.
     * @param d2 The second LocalDateTime.
     * @return The LocalDateTime that is later.
     */
    private LocalDateTime max(LocalDateTime d1, LocalDateTime d2) {
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
    private LocalDateTime min(LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null) return d2;
        if (d2 == null) return d1;

        return d1.isBefore(d2) ? d1 : d2;
    }

}
