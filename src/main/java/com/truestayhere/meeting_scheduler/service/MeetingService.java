package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Complex data validation and security will be added later!
// More complex overlap/conflict checking will be added later!

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {
    private final MeetingRepository meetingRepository;

    // Basic CRUD functionality implementation:

    // CREATE
    @Transactional
    public Meeting createMeeting(Meeting meeting) throws IllegalAccessException {

        // --- (Add later) Input Validation --

        // --- Duplicates Check ---
        List<Meeting> existingMeetings = meetingRepository.findByLocation_idAndStartTimeAndEndTime(
                meeting.getLocation().getId(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );

        if(!existingMeetings.isEmpty()) {
            throw new IllegalAccessException("Meeting with the same location, start time and end time already exists.");
        }

        // --- (Add later) Conflict/Overlap Check ---

        // --- Save the Meeting ---
        return meetingRepository.save(meeting);
    }

    // READ - All
    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    // READ - By ID
    public Meeting getMeetingById(Long id) {
        return meetingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Meeting not found with id: " + id));
    }

    // UPDATE
    @Transactional
    public Meeting updateMeeting(Long id, Meeting meetingDetails) {
        Meeting existingMeeting = getMeetingById(id);

        // --- (Add later) Input Validation --

        // --- (Add later) Conflict/Overlap Check ---

        existingMeeting.setTitle(meetingDetails.getTitle());
        existingMeeting.setStartTime(meetingDetails.getStartTime());
        existingMeeting.setEndTime(meetingDetails.getEndTime());
        existingMeeting.setLocation(meetingDetails.getLocation());
        existingMeeting.setAttendees(meetingDetails.getAttendees());

        return meetingRepository.save(existingMeeting);
    }

    // DELETE
    @Transactional
    public void deleteMeeting(Long id) {
        Meeting existingMeeting = getMeetingById(id);
        meetingRepository.delete(existingMeeting);
    }
}
