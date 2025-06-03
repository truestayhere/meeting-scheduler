package com.truestayhere.meeting_scheduler.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.dto.request.*;
import com.truestayhere.meeting_scheduler.dto.response.AttendeeDTO;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Specific helper for Attendee controller tests
 */
public class AttendeeTestHelper extends MockMvcTestHelper {

    private static final String ATTENDEES_ENDPOINT = "/api/attendees";

    public AttendeeTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    // Request methods
    public ResultActions performCreateAttendee(CreateAttendeeRequestDTO request) throws Exception {
        return performCreate(ATTENDEES_ENDPOINT, request);
    }

    public ResultActions performUpdateAttendee(Long id, UpdateAttendeeRequestDTO request) throws Exception {
        return performUpdate(ATTENDEES_ENDPOINT, id, request);
    }

    public ResultActions performGetAllAttendees() throws Exception {
        return performGetAll(ATTENDEES_ENDPOINT);
    }

    public ResultActions performGetAttendee(Long id) throws Exception {
        return performGetById(ATTENDEES_ENDPOINT, id);
    }

    public ResultActions performDeleteAttendee(Long id) throws Exception {
        return performDeleteById(ATTENDEES_ENDPOINT, id);
    }

    public ResultActions performGetAttendeeAvailability(Long id, LocalDate date) throws Exception {
        return performGetAvailability(ATTENDEES_ENDPOINT, id, date);
    }

    public ResultActions performFindCommonAvailability(CommonAvailabilityRequestDTO request) throws Exception {
        return performPostRequest(ATTENDEES_ENDPOINT + "/common-availability", request);
    }

    // Response assertion methods
    public void assertAttendeeResponse(ResultActions resultActions, AttendeeDTO expected) throws Exception {
        assertEntityResponse(resultActions, expected, this::validateAttendeeFields);
    }

    public void assertCreatedAttendeeResponse(ResultActions resultActions, AttendeeDTO expected) throws Exception {
        assertCreatedEntityResponse(resultActions, expected, this::validateAttendeeFields);
    }

    public void assertAttendeeListResponse(ResultActions resultActions, List<AttendeeDTO> expected) throws Exception {
        assertEntityListResponse(resultActions, expected, this::validateAttendeeListFields);
    }

    // helper methods
    private void validateAttendeeFields(ResultActions resultActions, AttendeeDTO expected) throws Exception {
        resultActions
                .andExpect(jsonPath("$.id", is(expected.id().intValue())))
                .andExpect(jsonPath("$.name", is(expected.name())))
                .andExpect(jsonPath("$.email", is(expected.email())));
    }

    private void validateAttendeeListFields(ResultActions resultActions, List<AttendeeDTO> expected) throws Exception {
        for (int i = 0; i < expected.size(); i++) {
            AttendeeDTO attendee = expected.get(i);
            resultActions
                    .andExpect(jsonPath("$[" + i + "].id", is(attendee.id().intValue())))
                    .andExpect(jsonPath("$[" + i + "].name", is(attendee.name())))
                    .andExpect(jsonPath("$[" + i + "].email", is(attendee.email())));
        }
    }
}
