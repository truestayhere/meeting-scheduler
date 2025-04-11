package com.truestayhere.meeting_scheduler.controller;


import com.truestayhere.meeting_scheduler.dto.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.MeetingDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private MeetingService meetingService;

    // GET /api/meetings - Get all meetings
    public ResponseEntity<List<MeetingDTO>> getAllMeetings() {
        List<MeetingDTO> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(meetings); // 200 OK
    }

    // GET /api/meetings/id - Get meeting by ID
    public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.getMeetingById(id);
        return ResponseEntity.ok(meeting); // 200 OK
    }

    // POST /api/meetings - Create a new meeting
    public ResponseEntity<MeetingDTO> createMeeting(@RequestBody CreateMeetingRequestDTO requestDTO) {
        MeetingDTO createdMeeting = meetingService.createMeeting(requestDTO);
        return new ResponseEntity<>(createdMeeting, HttpStatus.CREATED); // 201 CREATED
    }

    // PUT /api/meetings/id - Update meeting by ID
    public ResponseEntity<MeetingDTO> updateMeetingById(@PathVariable Long id, @RequestBody UpdateMeetingRequestDTO requestDTO) {
        MeetingDTO updatedMeeting = meetingService.updateMeeting(id, requestDTO);
        return ResponseEntity.ok(updatedMeeting); // 200 OK
    }

    // DELETE /api/meetings/id - Delete meeting by ID
    public ResponseEntity<Void> deleteMeetingById(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build(); // 204 NO CONTENT
    }


}
