package com.truestayhere.meeting_scheduler.controller;


import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.MeetingSuggestionRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationTimeSlotDTO;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import com.truestayhere.meeting_scheduler.service.MeetingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MeetingController {
    private final MeetingService meetingService;
    private final AvailabilityService availabilityService;

    // GET /api/meetings - Get all meetings
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<MeetingDTO>> getAllMeetings() {
        List<MeetingDTO> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(meetings); // 200 OK
    }

    // GET /api/meetings/id - Get a meeting by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.getMeetingById(id);
        return ResponseEntity.ok(meeting); // 200 OK
    }

    // GET /api/meetings/byAttendee/{attendeeId}?start=...&end=...
    // DateTimeFormat expects format like 2024-07-30T10:00:00
    @GetMapping("/byAttendee/{attendeeId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByAttendeeAndRange(
            @PathVariable Long attendeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<MeetingDTO> meetings = availabilityService.getMeetingsForAttendeeInRange(attendeeId, start, end);
        return ResponseEntity.ok(meetings); // 200 OK
    }

    // GET /api/meetings/byLocation/{locationId}?start=...&end=...
    @GetMapping("/byLocation/{locationId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByLocationAndRange(
            @PathVariable Long locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<MeetingDTO> meetings = availabilityService.getMeetingsForLocationInRange(locationId, start, end);
        return ResponseEntity.ok(meetings);
    }

    // POST /api/meetings - Create a new meeting
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<MeetingDTO> createMeeting(@Valid @RequestBody CreateMeetingRequestDTO requestDTO) {
        MeetingDTO createdMeeting = meetingService.createMeeting(requestDTO);
        return new ResponseEntity<>(createdMeeting, HttpStatus.CREATED); // 201 CREATED
    }

    // PUT /api/meetings/id - Update meeting by ID
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<MeetingDTO> updateMeetingById(@PathVariable Long id, @Valid @RequestBody UpdateMeetingRequestDTO requestDTO) {
        MeetingDTO updatedMeeting = meetingService.updateMeeting(id, requestDTO);
        return ResponseEntity.ok(updatedMeeting); // 200 OK
    }

    // DELETE /api/meetings/id - Delete meeting by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteMeetingById(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build(); // 204 NO CONTENT
    }

    // POST /api/meetings/suggestions - Find meeting suggestions
    @PostMapping("/suggestions")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<LocationTimeSlotDTO>> findMeetingSuggestions(
            @Valid @RequestBody MeetingSuggestionRequestDTO request) {
        List<LocationTimeSlotDTO> meetingSuggestions = availabilityService.findMeetingSuggestions(request);
        return ResponseEntity.ok(meetingSuggestions); // 200 OK
    }
}
