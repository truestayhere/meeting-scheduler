package com.truestayhere.meeting_scheduler.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truestayhere.meeting_scheduler.AbstractIntegrationTest;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Role;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuthenticationFlowIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void fullAuthenticationAndAuthorizationFlow() throws Exception {
        Attendee adminUser = new Attendee(
                "Test Admin",
                "admin@test.com",
                passwordEncoder.encode("password"),
                Role.ADMIN);
        attendeeRepository.save(adminUser);

        String credentials = "{\"email\":\"admin@test.com\", \"password\":\"password\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        java.util.Map<String, String> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
        });

        String token = responseMap.get("token");

        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isUnauthorized());
    }
}
