package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Data validation and security will be added later!

@Service
@RequiredArgsConstructor // Lombok generates constructor with fields that are final
@Transactional(readOnly = true) // This annotation ensures that transactions initiated by the service are atomic.
// (readOnly = true) sets default reading mode to optimize operations with data.
public class AttendeeService {
    private final AttendeeRepository attendeeRepository;

    // Basic CRUD functionality implementation:

    // CREATE
    @Transactional // It means that this method modifies data
    public Attendee createAttendee(Attendee attendee) throws IllegalAccessException {
        if (attendeeRepository.findByEmail(attendee.getEmail()).isPresent()) {
            throw new IllegalAccessException("Attendee with email " + attendee.getEmail() + " already exists.");
        }
        return attendeeRepository.save(attendee);
    }

    // READ - All
    public List<Attendee> getAllAttendees() {
        return attendeeRepository.findAll();
    }

    // READ - By ID
    public Attendee getAttendeeById(Long id) {
        return attendeeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Attendee not found with id: " + id));
    }

    // UPDATE
    @Transactional
    public Attendee updateAttendee(Long id, Attendee attendeeDetails) {
        Attendee existingAttendee = getAttendeeById(id);

        existingAttendee.setName(attendeeDetails.getName());
        // (Add later) Need to check if the email is unique before updating!
        existingAttendee.setEmail(attendeeDetails.getEmail());

        return attendeeRepository.save(existingAttendee);
    }

    // DELETE
    @Transactional
    public void deleteAttendee(Long id) {
        Attendee existingAttendee = getAttendeeById(id); // if attendee is not found throws EntityNotFoundException
        attendeeRepository.delete(existingAttendee);
    }


}
