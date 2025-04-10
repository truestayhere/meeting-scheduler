package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.*;
import com.truestayhere.meeting_scheduler.mapper.MeetingMapper;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Complex data validation and security will be added later!
// More complex overlap/conflict checking will be added later!

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final LocationRepository locationRepository;
    private final AttendeeRepository attendeeRepository;
    private final MeetingMapper meetingMapper;

    // Basic CRUD functionality implementation:

    // CREATE - Accepts a CreateMeetingRequestDTO, returns MeetingDTO
    @Transactional
    public MeetingDTO createMeeting(CreateMeetingRequestDTO requestDTO) throws IllegalArgumentException {

        // --- (Add later) Input Validation --

        // --- Duplicates Check ---
        List<Meeting> existingMeetings = meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                requestDTO.locationId(),
                requestDTO.startTime(),
                requestDTO.endTime()
        );

        if(!existingMeetings.isEmpty()) {
            throw new IllegalArgumentException("Meeting with the same location, start time and end time already exists.");
        }

        // --- (Add later) Conflict/Overlap Check ---

        // --- Fetch Location and Attendees Data ---

        Location location = findLocationEntityById(requestDTO.locationId());
        Set<Attendee> attendees = findAttendeesById(requestDTO.attendeeIds());

        // --- Save the Meeting ---

        Meeting newMeeting = new Meeting();
        newMeeting.setTitle(requestDTO.title());
        newMeeting.setStartTime(requestDTO.startTime());
        newMeeting.setEndTime(requestDTO.endTime());
        newMeeting.setLocation(location);
        newMeeting.setAttendees(attendees);

        Meeting savedMeeting = meetingRepository.save(newMeeting);
        return meetingMapper.mapToMeetingDTO(savedMeeting);
    }

    // READ - All - Returns List<MeetingDTO>
    public List<MeetingDTO> getAllMeetings() {
        List<Meeting> meetings = meetingRepository.findAll();
        return meetingMapper.mapToMeetingDTOList(meetings);
    }

    // READ - By ID - Accepts ID, returns MeetingDTO
    public MeetingDTO getMeetingById(Long id) {
        Meeting foundMeeting = findMeetingEntityById(id);
        return meetingMapper.mapToMeetingDTO(foundMeeting);
    }

    // Helper method - Accepts ID, returns Meeting Entity
    private Meeting findMeetingEntityById(Long id) {
        return meetingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Meeting not found with id: " + id));
    }

    // Helper method - Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
    }

    // Helper method - Accepts Set<ID>, returns Set<Attendee>
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

    // UPDATE - Accepts ID and UpdateMeetingRequestDTO, returns MeetingDTO
    @Transactional
    public MeetingDTO updateMeeting(Long id, UpdateMeetingRequestDTO requestDTO) {
        Meeting existingMeeting = findMeetingEntityById(id);

        // --- (Add later) Input Validation --

        // --- (Add later) Conflict/Overlap Check ---

        // --- Fetch Location and Attendees Data ---

        Location location = findLocationEntityById(requestDTO.locationId());
        Set<Attendee> attendees = findAttendeesById(requestDTO.attendeeIds());

        // --- Save the Meeting ---

        existingMeeting.setTitle(requestDTO.title());
        existingMeeting.setStartTime(requestDTO.startTime());
        existingMeeting.setEndTime(requestDTO.endTime());
        existingMeeting.setLocation(location);
        existingMeeting.setAttendees(attendees);

        Meeting savedMeeting = meetingRepository.save(existingMeeting);
        return meetingMapper.mapToMeetingDTO(savedMeeting);
    }

    // DELETE - Accepts ID
    @Transactional
    public void deleteMeeting(Long id) {
        if (!meetingRepository.existsById(id)) {
            throw new EntityNotFoundException("Meeting not found with id: " + id);
        }
        meetingRepository.deleteById(id);
    }

}
