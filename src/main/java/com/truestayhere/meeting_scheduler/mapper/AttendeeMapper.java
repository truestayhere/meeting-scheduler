package com.truestayhere.meeting_scheduler.mapper;


import com.truestayhere.meeting_scheduler.dto.AttendeeDTO;
import com.truestayhere.meeting_scheduler.dto.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.model.Attendee;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component // Marks this class as a bean
public class AttendeeMapper {

    // Map from Attendee Entity to AttendeeDTO
    public AttendeeDTO mapToAttendeeDTO(Attendee attendee) {
        if (attendee == null) {
            return null;
        }
        return new AttendeeDTO(
                attendee.getId(),
                attendee.getName(),
                attendee.getEmail()
        );
    }

    // Map from List<Attendee> to List<AttendeeDTO>
    public List<AttendeeDTO> mapToAttendeeDTOList(List<Attendee> attendees) {
        if (attendees == null) {
            return List.of(); // Returns empty list
        }
        return attendees.stream()
                .map(this::mapToAttendeeDTO)
                .collect(Collectors.toList());
    }

    // Map from Set<Attendee> to Set<AttendeeDTO>
    public Set<AttendeeDTO> mapToAttendeeDTOSet(Set<Attendee> attendees) {
        if (attendees == null) {
            return Set.of();
        }
        return attendees.stream()
                .map(this::mapToAttendeeDTO)
                .collect(Collectors.toSet());
    }

    // Map from CreateAttendeeRequestDTO to Attendee Entity
    public Attendee mapToAttendee(CreateAttendeeRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Attendee attendee = new Attendee(
                requestDTO.name(),
                requestDTO.email()
        );

        // Default working start time 9:00
        attendee.setWorkingStartTime(requestDTO.workingStartTime());

        // Default working end time 17:00
        attendee.setWorkingEndTime(requestDTO.workingEndTime());

        return attendee;
    }

    // Map from UpdateAttendeeRequestDTO to Attendee Entity
    public Attendee mapToAttendee(UpdateAttendeeRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Attendee attendee = new Attendee(
                requestDTO.name(),
                requestDTO.email()
        );

        // Default working start time 9:00
        attendee.setWorkingStartTime(requestDTO.workingStartTime());

        // Default working end time 17:00
        attendee.setWorkingEndTime(requestDTO.workingEndTime());

        return attendee;
    }
}
