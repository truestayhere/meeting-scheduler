package com.truestayhere.meeting_scheduler.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.dto.request.*;
import com.truestayhere.meeting_scheduler.dto.response.MeetingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Specific helper for Meeting controller tests
 */
public class MeetingTestHelper extends MockMvcTestHelper {

    private static final String MEETINGS_ENDPOINT = "/api/meetings";

    public MeetingTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    // Request methods
    public ResultActions performCreateMeeting(CreateMeetingRequestDTO request) throws Exception {
        return performCreate(MEETINGS_ENDPOINT, request);
    }

    public ResultActions performUpdateMeeting(Long id, UpdateMeetingRequestDTO request) throws Exception {
        return performUpdate(MEETINGS_ENDPOINT, id, request);
    }

    public ResultActions performGetAllMeetings() throws Exception {
        return performGetAll(MEETINGS_ENDPOINT);
    }

    public ResultActions performGetMeeting(Long id) throws Exception {
        return performGetById(MEETINGS_ENDPOINT, id);
    }

    public ResultActions performGetMeetingsByAttendeeAndRange(Long id, LocalDateTime rangeStart, LocalDateTime rangeEnd) throws Exception {
        return performGetMeetingsBy(MEETINGS_ENDPOINT + "/byAttendee", id, rangeStart, rangeEnd);
    }

    public ResultActions performGetMeetingsByLocationAndRange(Long id, LocalDateTime rangeStart, LocalDateTime rangeEnd) throws Exception {
        return performGetMeetingsBy(MEETINGS_ENDPOINT + "/byLocation", id, rangeStart, rangeEnd);
    }

    public ResultActions performDeleteMeeting(Long id) throws Exception {
        return performDeleteById(MEETINGS_ENDPOINT, id);
    }

    public ResultActions performFindMeetingSuggestions(MeetingSuggestionRequestDTO requestDTO) throws Exception {
        return performPostRequest(MEETINGS_ENDPOINT + "/suggestions", requestDTO);
    }

    // Response assertion methods using custom validators
    public void assertMeetingResponse(ResultActions resultActions, MeetingDTO expected) throws Exception {
        assertEntityResponse(resultActions, expected, this::validateMeetingFields);
    }

    public void assertCreatedMeetingResponse(ResultActions resultActions, MeetingDTO expected) throws Exception {
        assertCreatedEntityResponse(resultActions, expected, this::validateMeetingFields);
    }

    public void assertMeetingListResponse(ResultActions resultActions, List<MeetingDTO> expected) throws Exception {
        assertEntityListResponse(resultActions, expected, this::validateMeetingListFields);
    }

    // helper methods
    private void validateMeetingFields(ResultActions resultActions, MeetingDTO expected) throws Exception {
        resultActions
                .andExpect(jsonPath("$.id", is(expected.id().intValue())))
                .andExpect(jsonPath("$.title", is(expected.title())))
                .andExpect(jsonPath("$.startTime", is(expected.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .andExpect(jsonPath("$.endTime", is(expected.endTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .andExpect(jsonPath("$.location.id", is(expected.location().id().intValue())))
                .andExpect(jsonPath("$.location.name", is(expected.location().name())))
                .andExpect(jsonPath("$.attendees", hasSize(expected.attendees().size())));
    }

    private void validateMeetingListFields(ResultActions resultActions, List<MeetingDTO> expected) throws Exception {
        for (int i = 0; i < expected.size(); i++) {
            MeetingDTO meeting = expected.get(i);
            resultActions
                    .andExpect(jsonPath("$[" + i + "].id", is(meeting.id().intValue())))
                    .andExpect(jsonPath("$[" + i + "].title", is(meeting.title())))
                    .andExpect(jsonPath("$[" + i + "].startTime", is(meeting.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                    .andExpect(jsonPath("$[" + i + "].endTime", is(meeting.endTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                    .andExpect(jsonPath("$[" + i + "].location.id", is(meeting.location().id().intValue())))
                    .andExpect(jsonPath("$[" + i + "].location.name", is(meeting.location().name())))
                    .andExpect(jsonPath("$[" + i + "].attendees", hasSize(meeting.attendees().size())));
        }
    }
}
