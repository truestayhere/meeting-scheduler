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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        // --- Save the Meeting ---

        log.debug("Attempting to save new meeting");

        Meeting newMeeting = new Meeting();
        newMeeting.setTitle(requestDTO.title());
        newMeeting.setStartTime(requestDTO.startTime());
        newMeeting.setEndTime(requestDTO.endTime());
        newMeeting.setLocation(location);
        newMeeting.setAttendees(attendees);

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

        // --- Save the Meeting ---

        existingMeeting.setTitle(requestDTO.title());
        existingMeeting.setStartTime(requestDTO.startTime());
        existingMeeting.setEndTime(requestDTO.endTime());
        existingMeeting.setLocation(location);
        existingMeeting.setAttendees(attendees);

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

}
