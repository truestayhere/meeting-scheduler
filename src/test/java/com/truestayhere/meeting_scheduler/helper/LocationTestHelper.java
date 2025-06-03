package com.truestayhere.meeting_scheduler.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.LocationAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Specific helper for Location controller tests
 */
public class LocationTestHelper extends MockMvcTestHelper {

    private static final String LOCATIONS_ENDPOINT = "/api/locations";

    public LocationTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    // Request methods
    public ResultActions performCreateLocation(CreateLocationRequestDTO request) throws Exception {
        return performCreate(LOCATIONS_ENDPOINT, request);
    }

    public ResultActions performUpdateLocation(Long id, UpdateLocationRequestDTO request) throws Exception {
        return performUpdate(LOCATIONS_ENDPOINT, id, request);
    }

    public ResultActions performGetAllLocations() throws Exception {
        return performGetAll(LOCATIONS_ENDPOINT);
    }

    public ResultActions performGetLocation(Long id) throws Exception {
        return performGetById(LOCATIONS_ENDPOINT, id);
    }

    public ResultActions performDeleteLocation(Long id) throws Exception {
        return performDeleteById(LOCATIONS_ENDPOINT, id);
    }

    public ResultActions performGetLocationAvailability(Long id, LocalDate date) throws Exception {
        return performGetAvailability(LOCATIONS_ENDPOINT, id, date);
    }

    public ResultActions performAvailabilityByDuration(LocationAvailabilityRequestDTO request) throws Exception {
        return performPostRequest(LOCATIONS_ENDPOINT + "/availability-by-duration", request);
    }

    // Response assertion methods
    public void assertLocationResponse(ResultActions resultActions, LocationDTO expected) throws Exception {
        assertEntityResponse(resultActions, expected, this::validateLocationFields);
    }

    public void assertCreatedLocationResponse(ResultActions resultActions, LocationDTO expected) throws Exception {
        assertCreatedEntityResponse(resultActions, expected, this::validateLocationFields);
    }

    public void assertLocationListResponse(ResultActions resultActions, List<LocationDTO> expected) throws Exception {
        assertEntityListResponse(resultActions, expected, this::validateLocationListFields);
    }

    // helper methods
    private void validateLocationFields(ResultActions resultActions, LocationDTO expected) throws Exception {
        resultActions
                .andExpect(jsonPath("$.id", is(expected.id().intValue())))
                .andExpect(jsonPath("$.name", is(expected.name())))
                .andExpect(jsonPath("$.capacity", is(expected.capacity())));
    }

    private void validateLocationListFields(ResultActions resultActions, List<LocationDTO> expected) throws Exception {
        for (int i = 0; i < expected.size(); i++) {
            LocationDTO location = expected.get(i);
            resultActions
                    .andExpect(jsonPath("$["+ i + "].id", is(location.id().intValue())))
                    .andExpect(jsonPath("$["+ i + "].name", is(location.name())))
                    .andExpect(jsonPath("$["+ i + "].capacity", is(location.capacity())));
        }
    }

}