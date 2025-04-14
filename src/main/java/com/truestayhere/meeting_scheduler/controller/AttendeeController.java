package com.truestayhere.meeting_scheduler.controller;


import com.truestayhere.meeting_scheduler.dto.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.service.AttendeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // handles HTTP requests, returns JSON body
@RequestMapping("/api/attendees")
@RequiredArgsConstructor
public class AttendeeController {
    private final AttendeeService attendeeService;

    // GET /api/attendees - Get all attendees
    @GetMapping
    public ResponseEntity<List<AttendeeDTO>> getAllAttendees() {
        List<AttendeeDTO> attendees= attendeeService.getAllAttendees();
        return ResponseEntity.ok(attendees); // Returns 200 OK status code with the list of attendees DTOs
        // equivalent to: return new ResponseEntity<>(attendees, HttpStatus.OK);
    }

    // GET /api/attendees/id - Get attendee by ID
    @GetMapping("/{id}")
    public ResponseEntity<AttendeeDTO> getAttendeeById(@PathVariable Long id) {
        AttendeeDTO attendee = attendeeService.getAttendeeById(id);
        return ResponseEntity.ok(attendee);
    }

    // POST api/attendees - Create a new attendee
    @PostMapping
    public ResponseEntity<AttendeeDTO> createAttendee(@Valid @RequestBody CreateAttendeeRequestDTO requestDTO) {
        // @Valid tells Spring that if the input is not valid throw MethodArgumentNotValidException automatically
        // @RequestBody tells Spring to deserialize the JSON request body into the DTO
        AttendeeDTO createdAttendee = attendeeService.createAttendee(requestDTO);
        return new ResponseEntity<>(createdAttendee, HttpStatus.CREATED); // Returns 201 CREATED status code with the new attendee DTO
    }

    // PUT api/attendees/id - Update attendee by ID
    @PutMapping("/{id}")
    public ResponseEntity<AttendeeDTO> updateAttendee(@PathVariable Long id, @Valid @RequestBody UpdateAttendeeRequestDTO requestDTO) {
        AttendeeDTO updatedAttendee = attendeeService.updateAttendee(id, requestDTO);
        return ResponseEntity.ok(updatedAttendee); // Returns 200 OK status code with location DTO
    }

    // DELETE api/attendees/id - Delete attendee by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendee(@PathVariable Long id) {
        attendeeService.deleteAttendee(id);
        return ResponseEntity.noContent().build(); // Returns 204 NO CONTENT status code with no response body
    }

}
