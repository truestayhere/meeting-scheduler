package com.truestayhere.meeting_scheduler.dto.validation;

import com.truestayhere.meeting_scheduler.dto.request.CreateMeetingRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateMeetingRequestDTO;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class StartBeforeEndValidatorTest {

    private StartBeforeEndValidator validator;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    // Test Date: 14.08.xxxx (current year + 1)
    private final LocalDate DEFAULT_DATE = LocalDate.of(Year.now().getValue() + 1, 8, 14);

    @BeforeEach
    void setUp() {
        validator = new StartBeforeEndValidator();
    }

    @ParameterizedTest(name = "Start: {0}, End: {1}, Expected: {2}")
    @CsvSource({
            "09:00, 17:00, true",
            "10:00, 09:00, false",
            "12:00, 12:00, false"
    })
    void workingHoursValidationScenarios(String startTimeStr, String endTimeStr, boolean expectedResult) {
        LocalDateTime startTime = DEFAULT_DATE.atTime(LocalTime.parse(startTimeStr));
        LocalDateTime endTime = DEFAULT_DATE.atTime(LocalTime.parse(endTimeStr));

        CreateMeetingRequestDTO dto = new CreateMeetingRequestDTO(
                "Test Meeting",
                startTime,
                endTime,
                1L,
                Set.of(1L)
        );

        boolean actualResult = validator.isValid(dto, constraintValidatorContext);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    void isValid_whenEitherTimeIsNull_shouldReturnTrue() {
        UpdateMeetingRequestDTO dtoWithNullStart = new UpdateMeetingRequestDTO(
                "Null Start",
                null,
                DEFAULT_DATE.atTime(13,0),
                1L,
                Set.of(1L)
        );

        UpdateMeetingRequestDTO dtoWithNullEnd = new UpdateMeetingRequestDTO(
                "Null End",
                DEFAULT_DATE.atTime(13,0),
                null,
                1L,
                Set.of(1L)
        );

        UpdateMeetingRequestDTO dtoWithBothNull = new UpdateMeetingRequestDTO(
                "Both Null",
                null,
                null,
                1L,
                Set.of(1L)
        );

        assertThat(validator.isValid(dtoWithNullStart, constraintValidatorContext)).isTrue();
        assertThat(validator.isValid(dtoWithNullEnd, constraintValidatorContext)).isTrue();
        assertThat(validator.isValid(dtoWithBothNull, constraintValidatorContext)).isTrue();
    }
}
