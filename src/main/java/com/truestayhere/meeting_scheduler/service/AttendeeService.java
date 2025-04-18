package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.mapper.AttendeeMapper;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor // Lombok generates constructor with fields that are final
@Transactional(readOnly = true) // This annotation ensures that transactions initiated by the service are atomic.
// (readOnly = true) sets default reading mode to optimize operations with data.
public class AttendeeService {
    private final AttendeeRepository attendeeRepository;
    private final AttendeeMapper attendeeMapper;

    private static final Logger log = LoggerFactory.getLogger(AttendeeService.class);


    // === CRUD METHODS ===


    /**
     * Creates an attendee based on provided data.
     *
     * @param requestDTO A CreateAttendeeRequestDTO with the attendee data.
     * @return An AttendeeDTO with the created attendee data.
     */
    @Transactional // It means that this method modifies data
    public AttendeeDTO createAttendee(CreateAttendeeRequestDTO requestDTO) {
        log.debug("Attempting to create attendee with email: {}", requestDTO.email());

        // --- Duplicates Check ---

        checkDuplicateAttendee(requestDTO.email(), null);

        // --- Save the Attendee ---

        Attendee newAttendee = attendeeMapper.mapToAttendee(requestDTO);

        Attendee savedAttendee = attendeeRepository.save(newAttendee);

        log.info("Successfully created attendee with ID: {}", savedAttendee.getId());
        return attendeeMapper.mapToAttendeeDTO(savedAttendee);
    }


    /**
     * Fetches all attendees.
     *
     * @return A list of AttendeeDTOs for all attendees.
     */
    public List<AttendeeDTO> getAllAttendees() {
        List<Attendee> attendees = attendeeRepository.findAll();
        return attendeeMapper.mapToAttendeeDTOList(attendees);
    }


    /**
     * Fetches an attendee based on provided ID.
     *
     * @param id The ID of the attendee.
     * @return An AttendeeDTO for the found attendee.
     */
    public AttendeeDTO getAttendeeById(Long id) {
        Attendee foundAttendee = findAttendeeEntityById(id);
        return attendeeMapper.mapToAttendeeDTO(foundAttendee);
    }


    /**
     * Updates an attendee based on provided ID and update data.
     *
     * @param id         The ID og the attendee to be updated.
     * @param requestDTO The UpdateAttendeeRequestDTO with the updated data.
     * @return An AttendeeDTO for the updated attendee.
     */
    @Transactional
    public AttendeeDTO updateAttendee(Long id, UpdateAttendeeRequestDTO requestDTO) {
        log.debug("Attempting to update attendee with ID: {}", id);

        Attendee existingAttendee = findAttendeeEntityById(id);

        // --- Duplicates Check ---

        checkDuplicateAttendee(requestDTO.email(), id);

        // --- Save the Attendee ---

        existingAttendee.setName(requestDTO.name());
        existingAttendee.setEmail(requestDTO.email());

        Attendee savedAttendee = attendeeRepository.save(existingAttendee);

        log.info("Successfully updated attendee with ID: {}", id);
        return attendeeMapper.mapToAttendeeDTO(savedAttendee);
    }


    /**
     * Deletes an attendee by ID.
     *
     * @param id The ID of the attendee.
     */
    @Transactional
    public void deleteAttendee(Long id) {
        log.debug("Attempting to delete attendee with ID: {}", id);

        if (!attendeeRepository.existsById(id)) {
            throw new EntityNotFoundException("Attendee not found with ID: " + id);
        }

        log.info("Successfully deleted attendee with ID: {}", id);
        attendeeRepository.deleteById(id);
    }


    // === HELPER METHODS ===


    // Accepts ID, returns Attendee Entity
    private Attendee findAttendeeEntityById(Long id) {
        return attendeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendee not found with ID: " + id));
    }


    // Attendee Duplicates Check - Accepts String, ID, throws IllegalArgumentException
    private void checkDuplicateAttendee(String email, Long idToExclude) {

        log.debug("Checking duplicates for attendee with email: {}", email);

        Optional<Attendee> duplicateAttendeeOpt = attendeeRepository.findByEmail(email);

        if (duplicateAttendeeOpt.isPresent()) {
            Attendee duplicateAttendee = duplicateAttendeeOpt.get();

            if (idToExclude == null || !duplicateAttendee.getId().equals(idToExclude)) {
                String errorMessage = (idToExclude == null)
                        ? "Attendee with email " + email + " already exists."
                        : "Another attendee (" + duplicateAttendee.getId() + ") already exists with email " + email + ".";
                log.warn(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }

        log.debug("No duplicates found for the provided attendee email.");
    }

}
