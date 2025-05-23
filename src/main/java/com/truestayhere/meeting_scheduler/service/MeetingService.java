package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MeetingService {
    // Default working hours
    private static final LocalTime DEFAULT_WORKING_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORKING_END_TIME = LocalTime.of(17, 0);
    private final MeetingRepository meetingRepository;
    private final LocationRepository locationRepository;
    private final AttendeeRepository attendeeRepository;
    private final MeetingMapper meetingMapper;
    private final LocationMapper locationMapper;

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

        // --- Fetch Location and Attendees Data ---

        Location location = findLocationEntityById(requestDTO.locationId());
        log.debug("Fetched location entity with ID: {}", location.getId());

        Set<Attendee> attendees = findAttendeesById(requestDTO.attendeeIds());
        log.debug("Fetched {} attendee entities", attendees.size());

        // --- Duplicates Check ---

        checkMeetingDuplicates(requestDTO.locationId(), requestDTO.startTime(), requestDTO.endTime(), null);

        // --- Conflict/Overlap Check ---

        log.debug("Performing conflict checks before creating meeting.");
        checkLocationConflict(requestDTO.locationId(), requestDTO.startTime(), requestDTO.endTime(), null);
        checkAttendeeConflicts(requestDTO.attendeeIds(), requestDTO.startTime(), requestDTO.endTime(), null);
        log.debug("Conflict checks passed");

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

        LocalDateTime effectiveStartTime = (requestDTO.startTime() != null) ? requestDTO.startTime() : existingMeeting.getStartTime();
        LocalDateTime effectiveEndTime = (requestDTO.endTime() != null) ? requestDTO.endTime() : existingMeeting.getEndTime();
        Long effectiveLocationId = (requestDTO.locationId() != null) ? requestDTO.locationId() : existingMeeting.getLocation().getId();
        Set<Long> effectiveAttendeeIds = (requestDTO.attendeeIds() != null) ?
                requestDTO.attendeeIds() :
                existingMeeting.getAttendees()
                        .stream()
                        .map(Attendee::getId)
                        .collect(Collectors.toSet());

        // --- Fetch Location and Attendees Data ---

        Location location;
        if (requestDTO.locationId() != null) {
            location = findLocationEntityById(requestDTO.locationId());
            log.debug("Fetched location entity with ID: {}", location.getId());
        } else {
            location = existingMeeting.getLocation();
        }

        Set<Attendee> attendees;
        if (requestDTO.attendeeIds() != null) {
            attendees = findAttendeesById(requestDTO.attendeeIds());
            log.debug("Fetched {} attendee entities", attendees.size());
        } else {
            attendees = existingMeeting.getAttendees();
        }

        // --- Duplicates Check ---

        checkMeetingDuplicates(effectiveLocationId, effectiveStartTime, effectiveEndTime, id);

        // --- Conflict/Overlap Check ---

        log.debug("Performing conflict checks before updating meeting ID: {}", id);
        checkLocationConflict(effectiveLocationId, effectiveStartTime, effectiveEndTime, id);
        checkAttendeeConflicts(effectiveAttendeeIds, effectiveStartTime, effectiveEndTime, id);
        log.debug("Conflict checks passed for update");

        // --- Working Hours Check ---

        log.debug("Checking if updated meeting time is within location and attendees working hours.");
        checkMeetingWithinLocationWorkingHours(location, effectiveStartTime, effectiveEndTime);
        checkMeetingWithinAttendeesWorkingHours(attendees, effectiveStartTime, effectiveEndTime);
        log.debug("Update working hours checks passed");

        // --- Capacity Check ---

        log.debug("Checking if updated meeting location capacity is enough for the number of attendees.");
        checkLocationCapacity(location, attendees.size());
        log.debug("Update capacity check passed");

        // --- Updating Meeting ---

        if (requestDTO.title() != null) existingMeeting.setTitle(requestDTO.title());
        if (requestDTO.startTime() != null) existingMeeting.setStartTime(requestDTO.startTime());
        if (requestDTO.endTime() != null) existingMeeting.setEndTime(requestDTO.endTime());
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

        meetingRepository.deleteById(id);
        log.info("Successfully deleted meeting with ID: {}", id);
    }

    // === END CRUD METHODS ===

    // === HELPER METHODS ===

    // -- Fetch Methods ---


    // Accepts ID, returns Meeting Entity
    private Meeting findMeetingEntityById(Long id) {
        return meetingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Meeting not found with ID: " + id));
    }


    // Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with ID: " + id));
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

    // -- End Fetch Methods ---

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

    // --- Time Management Methods ---

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

    private record TimeWindow(LocalDateTime start, LocalDateTime end) {
    }

    // --- End of Time Management Methods ---
    // === END HELPER METHODS ===
}
