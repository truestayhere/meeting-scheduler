package com.truestayhere.meeting_scheduler.mapper;


import com.truestayhere.meeting_scheduler.dto.*;
import com.truestayhere.meeting_scheduler.model.Meeting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MeetingMapper {
    private final AttendeeMapper attendeeMapper;
    private final LocationMapper locationMapper;

    // Map from Meeting Entity to MeetingDTO
    public MeetingDTO mapToMeetingDTO(Meeting meeting) {
        LocationDTO locationDTO = locationMapper.mapToLocationDTO(meeting.getLocation());

        Set<AttendeeDTO> attendeeDTOs = attendeeMapper.mapToAttendeeDTOSet(meeting.getAttendees());

        return new MeetingDTO(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                locationDTO,
                attendeeDTOs
        );
    }

    // Map from List<Meeting> to List<MeetingDTO>
    public List<MeetingDTO> mapToMeetingDTOList(List<Meeting> meetings) {
        if (meetings == null) {
            return List.of(); // Returns empty list
        }
        return meetings.stream()
                .map(this::mapToMeetingDTO)
                .collect(Collectors.toList());
    }

    // Map from Set<Meeting> to Set<MeetingDTO>
    public Set<MeetingDTO> mapToMeetingDTOSet(Set<Meeting> meetings) {
        if (meetings == null) {
            return Set.of();
        }
        return meetings.stream()
                .map(this::mapToMeetingDTO)
                .collect(Collectors.toSet());
    }

}
