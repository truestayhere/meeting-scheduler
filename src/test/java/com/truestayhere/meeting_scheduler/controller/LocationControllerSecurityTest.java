package com.truestayhere.meeting_scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.config.CustomAuthenticationEntryPoint;
import com.truestayhere.meeting_scheduler.config.SecurityConfig;
import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.exception.GlobalExceptionHandler;
import com.truestayhere.meeting_scheduler.helper.LocationTestHelper;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import com.truestayhere.meeting_scheduler.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class LocationControllerSecurityTest {

    @MockitoBean
    LocationService locationService;
    @MockitoBean
    AvailabilityService availabilityService;
    @MockitoBean
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private LocationTestHelper locationTestHelper;
    private CreateLocationRequestDTO newLocation;
    private UpdateLocationRequestDTO updateLocation;
    private Long locationId;

    @BeforeEach
    void setUp() {
        locationTestHelper = new LocationTestHelper(mockMvc, objectMapper);
        locationId = 1L;

        newLocation = new CreateLocationRequestDTO(
                "Test Room",
                5,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        updateLocation = new UpdateLocationRequestDTO(
                "Updated Room",
                10,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );
    }

    static Stream<Arguments> locationOperationsProvider() {
        return Stream.of(
                Arguments.of("Create", HttpStatus.CREATED),
                Arguments.of("Update", HttpStatus.OK),
                Arguments.of("Delete", HttpStatus.NO_CONTENT)
        );
    }

    @ParameterizedTest(name = "{0}: should return {1}")
    @MethodSource("locationOperationsProvider")
    @WithMockUser(authorities = {"ADMIN"})
    void whenAdminPerformsLocationOperations_thenReturnsSuccess(
            String operationName,
            HttpStatus expectedStatus) throws Exception {

        ResultActions resultActions = executeOperation(operationName);
        resultActions.andExpect(status().is(expectedStatus.value()));
    }

    @ParameterizedTest(name = "{0}: should return FORBIDDEN")
    @MethodSource("locationOperationsProvider")
    @WithMockUser(authorities = {"USER"})
    void whenUserPerformsLocationOperations_thenReturnsForbidden(
            String operationName,
            HttpStatus expectedSuccessStatus) throws Exception {

        ResultActions resultActions = executeOperation(operationName);
        resultActions.andExpect(status().isForbidden());
    }

    private ResultActions executeOperation(String operation) throws Exception {
        return switch (operation) {
            case "Create" -> locationTestHelper.performCreateLocation(newLocation);
            case "Update" -> locationTestHelper.performUpdateLocation(locationId, updateLocation);
            case "Delete" -> locationTestHelper.performDeleteLocation(locationId);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

}