package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.request.CreateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateAttendeeRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AttendeeDTO;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.mapper.AttendeeMapper;
import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AttendeeServiceTest {

    private final Long DEFAULT_ATTENDEE_ID = 1L;
    private final String DEFAULT_ATTENDEE_NAME = "Attendee Name";
    private final String DEFAULT_ATTENDEE_EMAIL = "attendeename@test.com";
    private final String DEFAULT_RAW_PASSWORD = "password123";
    private final String DEFAULT_HASHED_PASSWORD = "hashedPasswordValue";
    @Mock
    private AttendeeRepository attendeeRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private AttendeeMapper attendeeMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AttendeeService attendeeService;
    private CreateAttendeeRequestDTO defaultCreateRequest;
    private UpdateAttendeeRequestDTO defaultUpdateRequest;
    private Attendee defaultAttendee;
    private Attendee defaultSavedAttendee;
    private AttendeeDTO defaultAttendeeDTO;

    @BeforeEach
    void setUp() {

        defaultCreateRequest = new CreateAttendeeRequestDTO(
                DEFAULT_ATTENDEE_NAME,
                DEFAULT_ATTENDEE_EMAIL,
                DEFAULT_RAW_PASSWORD,
                null,
                null,
                null
        );

        // All fields updated by default
        defaultUpdateRequest = new UpdateAttendeeRequestDTO(
                DEFAULT_ATTENDEE_NAME + " Updated",
                "attendeeupdated@test.com",
                DEFAULT_RAW_PASSWORD + "Update",
                "ROLE_ADMIN",
                LocalTime.of(12, 0),
                LocalTime.of(19, 0)
        );

        defaultAttendee = new Attendee();
        defaultAttendee.setId(DEFAULT_ATTENDEE_ID);
        defaultAttendee.setName(DEFAULT_ATTENDEE_NAME);
        defaultAttendee.setEmail(DEFAULT_ATTENDEE_EMAIL);
        defaultAttendee.setPassword(DEFAULT_HASHED_PASSWORD);

        defaultSavedAttendee = new Attendee();
        defaultAttendee.setId(DEFAULT_ATTENDEE_ID);
        defaultAttendee.setName(DEFAULT_ATTENDEE_NAME);
        defaultAttendee.setEmail(DEFAULT_ATTENDEE_EMAIL);
        defaultAttendee.setPassword(DEFAULT_HASHED_PASSWORD);

        defaultAttendeeDTO = new AttendeeDTO(
                DEFAULT_ATTENDEE_ID,
                DEFAULT_ATTENDEE_NAME,
                DEFAULT_ATTENDEE_EMAIL
        );
    }


    @Test
    void createAttendee_shouldReturnAttendeeDTO_whenSuccessful() {
        when(attendeeRepository.findByEmail(defaultCreateRequest.email())).thenReturn(Optional.empty());
        when(attendeeMapper.mapToAttendee(defaultCreateRequest)).thenReturn(defaultAttendee);
        when(passwordEncoder.encode(defaultCreateRequest.password())).thenReturn(DEFAULT_HASHED_PASSWORD);
        ArgumentCaptor<Attendee> attendeeCaptor = ArgumentCaptor.forClass(Attendee.class);
        when(attendeeRepository.save(attendeeCaptor.capture())).thenReturn(defaultSavedAttendee);
        when(attendeeMapper.mapToAttendeeDTO(defaultSavedAttendee)).thenReturn(defaultAttendeeDTO);

        AttendeeDTO result = attendeeService.createAttendee(defaultCreateRequest);

        assertNotNull(result);
        assertEquals(DEFAULT_ATTENDEE_ID, result.id());
        assertEquals(DEFAULT_ATTENDEE_NAME, result.name());
        assertEquals(DEFAULT_ATTENDEE_EMAIL, result.email());

        Attendee capturedAttendee = attendeeCaptor.getValue();
        assertNotNull(capturedAttendee);
        assertEquals(defaultCreateRequest.email(), capturedAttendee.getEmail());
        assertEquals(DEFAULT_HASHED_PASSWORD, capturedAttendee.getPassword(), "Password should be hashed before saving");
        assertNotEquals(DEFAULT_RAW_PASSWORD, capturedAttendee.getPassword(), "Raw password should never be saved");

        verify(attendeeRepository).findByEmail(defaultCreateRequest.email());
        verify(attendeeMapper).mapToAttendee(defaultCreateRequest);
        verify(passwordEncoder).encode(DEFAULT_RAW_PASSWORD);
        verify(attendeeRepository).save(any(Attendee.class));
        verify(attendeeMapper).mapToAttendeeDTO(defaultSavedAttendee);
    }


    @Test
    void createAttendee_shouldThrowIllegalArgumentException_whenEmailIsDuplicate() {
        String duplicateEmail = DEFAULT_ATTENDEE_EMAIL;
        String expectedErrorMessage = "Attendee with email '" + duplicateEmail + "' already exists.";
        when(attendeeRepository.findByEmail(duplicateEmail)).thenReturn(Optional.of(defaultAttendee));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            attendeeService.createAttendee(defaultCreateRequest);
        });
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(attendeeRepository).findByEmail(duplicateEmail);
        verify(attendeeMapper, never()).mapToAttendee(defaultCreateRequest);
        verify(passwordEncoder, never()).encode(DEFAULT_RAW_PASSWORD);
        verify(attendeeRepository, never()).save(any(Attendee.class));
        verify(attendeeMapper, never()).mapToAttendeeDTO(defaultSavedAttendee);
    }


    @Test
    void getAllLocations_shouldReturnListOfAttendeeDTOs_whenAttendeesExist() {
        Attendee attendee1 = new Attendee();
        attendee1.setId(DEFAULT_ATTENDEE_ID);
        attendee1.setName(DEFAULT_ATTENDEE_NAME);
        attendee1.setEmail(DEFAULT_ATTENDEE_EMAIL);
        attendee1.setPassword(DEFAULT_HASHED_PASSWORD);
        attendee1.setWorkingStartTime(LocalTime.of(7, 0));
        attendee1.setWorkingEndTime(LocalTime.of(15, 0));

        Attendee attendee2 = new Attendee();
        attendee1.setId(DEFAULT_ATTENDEE_ID + 1);
        attendee1.setName(DEFAULT_ATTENDEE_NAME + " (2)");
        attendee1.setEmail("attendeetwo@test.com");
        attendee1.setPassword(DEFAULT_HASHED_PASSWORD);
        attendee1.setWorkingStartTime(LocalTime.of(8, 0));
        attendee1.setWorkingEndTime(LocalTime.of(16, 0));

        List<Attendee> mockAttendees = Arrays.asList(attendee1, attendee2);

        AttendeeDTO dto1 = new AttendeeDTO(DEFAULT_ATTENDEE_ID, DEFAULT_ATTENDEE_NAME, DEFAULT_ATTENDEE_EMAIL);
        AttendeeDTO dto2 = new AttendeeDTO((DEFAULT_ATTENDEE_ID + 1), (DEFAULT_ATTENDEE_NAME + " (2)"), "attendeetwo@test.com");

        List<AttendeeDTO> mockAttendeeDTOs = Arrays.asList(dto1, dto2);

        when(attendeeRepository.findAll()).thenReturn(mockAttendees);
        when(attendeeMapper.mapToAttendeeDTOList(mockAttendees)).thenReturn(mockAttendeeDTOs);

        List<AttendeeDTO> results = attendeeService.getAllAttendees();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsAll(mockAttendeeDTOs) && mockAttendeeDTOs.containsAll(results));

        verify(attendeeRepository).findAll();
        verify(attendeeMapper).mapToAttendeeDTOList(mockAttendees);
        verify(attendeeMapper, never()).mapToAttendeeDTO(any(Attendee.class));
    }


    @Test
    void getAllAttendees_shouldReturnEmptyList_whenNoAttendeesExist() {
        List<Attendee> emptyAttendeesList = List.of();
        List<AttendeeDTO> emptyDTOList = List.of();

        when(attendeeRepository.findAll()).thenReturn(emptyAttendeesList);
        when(attendeeMapper.mapToAttendeeDTOList(emptyAttendeesList)).thenReturn(emptyDTOList);

        List<AttendeeDTO> results = attendeeService.getAllAttendees();

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(attendeeRepository).findAll();
        verify(attendeeMapper).mapToAttendeeDTOList(emptyAttendeesList);
    }


    @Test
    void getAttendeeById_shouldReturnAttendeeDTO_whenAttendeeExists() {
        Long attendeeId = DEFAULT_ATTENDEE_ID;
        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(defaultAttendee));
        when(attendeeMapper.mapToAttendeeDTO(defaultAttendee)).thenReturn(defaultAttendeeDTO);

        AttendeeDTO result = attendeeService.getAttendeeById(attendeeId);

        assertNotNull(result);
        assertEquals(attendeeId, result.id());
        assertEquals(DEFAULT_ATTENDEE_EMAIL, result.email());

        verify(attendeeRepository).findById(attendeeId);
        verify(attendeeMapper).mapToAttendeeDTO(defaultAttendee);
    }


    @Test
    void getAttendeeById_shouldThrowEntityNotFoundException_whenAttendeeDoesNotExist() {
        Long nonExistentAttendeeId = DEFAULT_ATTENDEE_ID;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(attendeeRepository.findById(nonExistentAttendeeId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> attendeeService.getAttendeeById(nonExistentAttendeeId)
        );

        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(attendeeRepository).findById(nonExistentAttendeeId);
        verify(attendeeMapper, never()).mapToAttendeeDTO(any(Attendee.class));
    }


    @Test
    void updateAttendee_shouldUpdateFieldsAndKeepPassword_whenPasswordNotInRequest() {
        Long attendeeId = DEFAULT_ATTENDEE_ID;

        UpdateAttendeeRequestDTO updateRequest = new UpdateAttendeeRequestDTO(
                "Updated Name", "updated@example.com", null, "ROLE_ADMIN",
                LocalTime.of(10, 0), LocalTime.of(18, 0)
        );

        Attendee existingAttendee = new Attendee();
        existingAttendee.setId(attendeeId);
        existingAttendee.setName("Original Name");
        existingAttendee.setEmail("original@example.com");
        existingAttendee.setPassword(DEFAULT_HASHED_PASSWORD);
        existingAttendee.setRole("ROLE_USER");
        existingAttendee.setWorkingStartTime(LocalTime.of(9, 0));
        existingAttendee.setWorkingEndTime(LocalTime.of(17, 0));

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(existingAttendee));
        when(attendeeRepository.findByEmail(updateRequest.email())).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            UpdateAttendeeRequestDTO dtoArg = invocation.getArgument(0);
            Attendee entityArg = invocation.getArgument(1);

            if (dtoArg.name() != null) entityArg.setName(dtoArg.name());
            if (dtoArg.email() != null) entityArg.setEmail(dtoArg.email());
            if (dtoArg.role() != null) entityArg.setRole(dtoArg.role());
            if (dtoArg.workingStartTime() != null) entityArg.setWorkingStartTime(dtoArg.workingStartTime());
            if (dtoArg.workingEndTime() != null) entityArg.setWorkingEndTime(dtoArg.workingEndTime());

            return null;
        }).when(attendeeMapper).updateAttendeeFromDto(eq(updateRequest), any(Attendee.class));

        AttendeeDTO expectedResponse = new AttendeeDTO(attendeeId, updateRequest.name(), updateRequest.email());

        when(attendeeMapper.mapToAttendeeDTO(any(Attendee.class))).thenReturn(expectedResponse);

        AttendeeDTO result = attendeeService.updateAttendee(attendeeId, updateRequest);

        ArgumentCaptor<Attendee> entityCaptor = ArgumentCaptor.forClass(Attendee.class);
        verify(attendeeMapper).mapToAttendeeDTO(entityCaptor.capture());

        Attendee capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity);
        assertEquals("Updated Name", capturedEntity.getName());
        assertEquals("updated@example.com", capturedEntity.getEmail());
        assertEquals(DEFAULT_HASHED_PASSWORD, capturedEntity.getPassword(), "Password should not have changed.");
        assertEquals("ROLE_ADMIN", capturedEntity.getRole());

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        verify(attendeeRepository).findById(attendeeId);
    }

    @Test
    void updateAttendee_shouldUpdateFieldsAndHashNewPassword_whenPasswordInRequest() {
        Long attendeeId = DEFAULT_ATTENDEE_ID;
        String newRawPassword = "newPassword123";
        String newHashedPassword = "hashed-newPassword123";

        UpdateAttendeeRequestDTO updateRequestWithPassword = new UpdateAttendeeRequestDTO(
                "Updated Name", "updated@example.com", newRawPassword, "ROLE_ADMIN",
                LocalTime.of(10, 0), LocalTime.of(18, 0)
        );

        Attendee existingAttendee = new Attendee();
        existingAttendee.setId(attendeeId);
        existingAttendee.setName("Original Name");
        existingAttendee.setEmail("original@example.com");
        existingAttendee.setPassword(DEFAULT_HASHED_PASSWORD); // Original password

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(existingAttendee));
        when(attendeeRepository.findByEmail(updateRequestWithPassword.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(newRawPassword)).thenReturn(newHashedPassword);
        doAnswer(invocation -> {
            UpdateAttendeeRequestDTO dtoArg = invocation.getArgument(0);
            Attendee entityArg = invocation.getArgument(1);

            if (dtoArg.name() != null) entityArg.setName(dtoArg.name());
            if (dtoArg.email() != null) entityArg.setEmail(dtoArg.email());
            if (dtoArg.role() != null) entityArg.setRole(dtoArg.role());
            if (dtoArg.workingStartTime() != null) entityArg.setWorkingStartTime(dtoArg.workingStartTime());
            if (dtoArg.workingEndTime() != null) entityArg.setWorkingEndTime(dtoArg.workingEndTime());

            return null;
        }).when(attendeeMapper).updateAttendeeFromDto(eq(updateRequestWithPassword), any(Attendee.class));

        AttendeeDTO expectedResponse = new AttendeeDTO(attendeeId, "Updated Name", "updated@example.com");
        when(attendeeMapper.mapToAttendeeDTO(any(Attendee.class))).thenReturn(expectedResponse);

        AttendeeDTO result = attendeeService.updateAttendee(attendeeId, updateRequestWithPassword);

        ArgumentCaptor<Attendee> entityCaptor = ArgumentCaptor.forClass(Attendee.class);
        verify(attendeeMapper).mapToAttendeeDTO(entityCaptor.capture());
        Attendee capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity);
        assertEquals("Updated Name", capturedEntity.getName());
        assertEquals("updated@example.com", capturedEntity.getEmail());
        assertEquals("ROLE_ADMIN", capturedEntity.getRole());
        assertEquals(newHashedPassword, capturedEntity.getPassword(), "Password should have been updated to the new hashed value.");
        assertEquals(expectedResponse, result);

        verify(attendeeRepository).findById(attendeeId);
        verify(passwordEncoder).encode(newRawPassword);
    }


    @Test
    void updateAttendee_shouldSucceed_whenDuplicatesCheckReturnsOnlyTheAttendeeBeingUpdated() {
        Long attendeeId = DEFAULT_ATTENDEE_ID;
        String originalEmail = "user@example.com";

        UpdateAttendeeRequestDTO updateRequest = new UpdateAttendeeRequestDTO(
                "New Name",
                null,
                null,
                null,
                null,
                null
        );

        Attendee existingAttendee = new Attendee();
        existingAttendee.setId(attendeeId);
        existingAttendee.setName("Original Name");
        existingAttendee.setEmail(originalEmail);
        existingAttendee.setPassword(DEFAULT_HASHED_PASSWORD);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(existingAttendee));
        when(attendeeRepository.findByEmail(originalEmail)).thenReturn(Optional.of(existingAttendee));
        doAnswer(invocation -> {
            UpdateAttendeeRequestDTO dtoArg = invocation.getArgument(0);
            Attendee entityArg = invocation.getArgument(1);
            if (dtoArg.name() != null) entityArg.setName(dtoArg.name());
            return null;
        }).when(attendeeMapper).updateAttendeeFromDto(eq(updateRequest), any(Attendee.class));

        AttendeeDTO expectedResponse = new AttendeeDTO(attendeeId, "New Name", originalEmail);
        when(attendeeMapper.mapToAttendeeDTO(any(Attendee.class))).thenReturn(expectedResponse);

        assertDoesNotThrow(() -> {
            attendeeService.updateAttendee(attendeeId, updateRequest);
        }, "Update should succeed when duplicate check finds the same user.");


        verify(attendeeRepository).findById(attendeeId);
        verify(attendeeRepository).findByEmail(originalEmail);
        verify(attendeeMapper).updateAttendeeFromDto(eq(updateRequest), any(Attendee.class));
        verify(attendeeMapper).mapToAttendeeDTO(any(Attendee.class));
    }


    @Test
    void updateAttendee_shouldThrowEntityNotFoundException_whenAttendeeDoesNotExist() {
        Long nonExistentAttendeeId = DEFAULT_ATTENDEE_ID;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(attendeeRepository.findById(nonExistentAttendeeId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> attendeeService.updateAttendee(nonExistentAttendeeId, defaultUpdateRequest)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(attendeeRepository).findById(nonExistentAttendeeId);
        verify(attendeeRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(attendeeMapper, never()).updateAttendeeFromDto(any(), any());
        verify(attendeeRepository, never()).save(any(Attendee.class));
        verify(attendeeMapper, never()).mapToAttendeeDTO(any(Attendee.class));
    }


    @Test
    void updateAttendee_shouldThrowIllegalArgumentException_whenEmailConflictsWithAnotherAttendee() {
        Long attendeeIdBeingUpdated = DEFAULT_ATTENDEE_ID + 1;
        Long otherAttendeeIdWithSameEmail = defaultAttendee.getId();
        String conflictingEmail = defaultAttendee.getEmail();

        UpdateAttendeeRequestDTO updateRequest = new UpdateAttendeeRequestDTO(
                defaultUpdateRequest.name(), conflictingEmail, null, null, null, null
        );

        Attendee attendeeBeingUpdated = new Attendee();
        attendeeBeingUpdated.setId(attendeeIdBeingUpdated);
        attendeeBeingUpdated.setEmail("oldemail@test.com");

        String expectedErrorMessage = "Another attendee (" +
                otherAttendeeIdWithSameEmail + ") already exists with email '"
                + conflictingEmail + "'.";

        when(attendeeRepository.findById(attendeeIdBeingUpdated)).thenReturn(Optional.of(attendeeBeingUpdated));
        when(attendeeRepository.findByEmail(conflictingEmail)).thenReturn(Optional.of(defaultAttendee));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> attendeeService.updateAttendee(attendeeIdBeingUpdated, updateRequest)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(attendeeRepository).findById(attendeeIdBeingUpdated);
        verify(attendeeRepository).findByEmail(conflictingEmail);
        verify(passwordEncoder, never()).encode(anyString());
        verify(attendeeMapper, never()).updateAttendeeFromDto(any(), any());
        verify(attendeeRepository, never()).save(any(Attendee.class));
        verify(attendeeMapper, never()).mapToAttendeeDTO(any(Attendee.class));
    }


    @Test
    void deleteAttendee_shouldDeleteAttendee_whenAttendeeExistsAndNotInUse() {
        Long attendeeIdToDelete = DEFAULT_ATTENDEE_ID;

        when(attendeeRepository.existsById(attendeeIdToDelete)).thenReturn(true);
        when(meetingRepository.findByAttendees_id(attendeeIdToDelete)).thenReturn(List.of());
        doNothing().when(attendeeRepository).deleteById(attendeeIdToDelete);

        assertDoesNotThrow(() -> attendeeService.deleteAttendee(attendeeIdToDelete));

        verify(attendeeRepository).existsById(attendeeIdToDelete);
        verify(meetingRepository).findByAttendees_id(attendeeIdToDelete);
        verify(attendeeRepository).deleteById(attendeeIdToDelete);
    }


    @Test
    void deleteAttendee_shouldThrowEntityNotFoundException_whenAttendeeDoesNotExist() {
        Long nonExistentAttendeeId = DEFAULT_ATTENDEE_ID;
        String expectedErrorMessage = "Attendee not found with ID: " + nonExistentAttendeeId;

        when(attendeeRepository.existsById(nonExistentAttendeeId)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> attendeeService.deleteAttendee(nonExistentAttendeeId)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(attendeeRepository).existsById(nonExistentAttendeeId);
        verify(meetingRepository, never()).findByAttendees_id(anyLong());
        verify(attendeeRepository, never()).deleteById(anyLong());
    }


    @Test
    void deleteAttendee_shouldThrowResourceInUseException_whenAttendeeIsInUseByMeetings() {
        Long attendeeIdToDelete = DEFAULT_ATTENDEE_ID;

        Meeting meeting1 = new Meeting();
        meeting1.setId(101L);
        Meeting meeting2 = new Meeting();
        meeting2.setId(102L);
        List<Meeting> conflictingMeetingsList = Arrays.asList(meeting1, meeting2);
        List<Long> expectedMeetingIds = Arrays.asList(101L, 102L);

        when(attendeeRepository.existsById(attendeeIdToDelete)).thenReturn(true);
        when(meetingRepository.findByAttendees_id(attendeeIdToDelete)).thenReturn(conflictingMeetingsList);

        String expectedErrorMessage = String.format(
                "Attendee cannot be deleted because they are included in %d meeting(s). See details.",
                expectedMeetingIds.size()
        );

        ResourceInUseException exception = assertThrows(
                ResourceInUseException.class,
                () -> attendeeService.deleteAttendee(attendeeIdToDelete)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());
        assertNotNull(exception.getConflictingResourceIds(), "Conflicting IDs should be present in the exception.");
        assertTrue(exception.getConflictingResourceIds().containsAll(expectedMeetingIds) && expectedMeetingIds.containsAll(exception.getConflictingResourceIds()), "Conflicting IDs in exception do not match expected IDs.");
        assertEquals(expectedMeetingIds.size(), exception.getConflictingResourceIds().size(), "Number of conflicting IDs doesn't match");

        verify(attendeeRepository).existsById(attendeeIdToDelete);
        verify(meetingRepository).findByAttendees_id(attendeeIdToDelete);
        verify(attendeeRepository, never()).deleteById(anyLong());
    }


}
