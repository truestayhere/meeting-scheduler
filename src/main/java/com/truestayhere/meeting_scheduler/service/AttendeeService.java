package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.mapper.AttendeeMapper;
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
    private final AttendeeMapper attendeeMapper;

    // Basic CRUD functionality implementation:

    // CREATE - Accepts a CreateAttendeeRequestDTO, returns AttendeeDTO
    @Transactional // It means that this method modifies data
    public AttendeeDTO createAttendee(CreateAttendeeRequestDTO requestDTO) throws IllegalArgumentException {
        if (attendeeRepository.findByEmail(requestDTO.email()).isPresent()) {
            throw new IllegalArgumentException("Attendee with email " + requestDTO.email() + " already exists.");
        }

        // --- (Add later) Input Validation --

        // --- Save the Attendee ---

        Attendee newAttendee = attendeeMapper.mapToAttendee(requestDTO);

        Attendee savedAttendee = attendeeRepository.save(newAttendee);
        return attendeeMapper.mapToAttendeeDTO(savedAttendee);
    }


    // READ - All - Returns List<AttendeeDTO>
    public List<AttendeeDTO> getAllAttendees() {
        List<Attendee> attendees = attendeeRepository.findAll();
        return attendeeMapper.mapToAttendeeDTOList(attendees);
    }

    // READ - By ID - Accepts ID, returns AttendeeDTO
    public AttendeeDTO getAttendeeById(Long id) {
        Attendee foundAttendee = findAttendeeEntityById(id);
        return attendeeMapper.mapToAttendeeDTO(foundAttendee);
    }

    // Helper method - Accepts ID, returns Attendee Entity
    private Attendee findAttendeeEntityById(Long id) {
        return attendeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendee not found with id: " + id));
    }

    // UPDATE - Accepts ID and UpdateAttendeeRequestDTO, returns AttendeeDTO
    @Transactional
    public AttendeeDTO updateAttendee(Long id, UpdateAttendeeRequestDTO requestDTO) {
        Attendee existingAttendee = findAttendeeEntityById(id);

        // --- (Add later) Input Validation --

        existingAttendee.setName(requestDTO.name());
        // (Add later) Need to check if the email is unique before updating!
        existingAttendee.setEmail(requestDTO.email());

        Attendee savedAttendee = attendeeRepository.save(existingAttendee);
        return attendeeMapper.mapToAttendeeDTO(savedAttendee);
    }

    // DELETE - Accepts ID
    @Transactional
    public void deleteAttendee(Long id) {
        if (!attendeeRepository.existsById(id)) {
            throw new EntityNotFoundException("Attendee not found with id: " + id);
        }
        attendeeRepository.deleteById(id);
    }


}
