package com.truestayhere.meeting_scheduler.dto.validation;


import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ValidWorkingHoursValidatorTest {

    private ValidWorkingHoursValidator validator;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;
    @Mock
    private ValidWorkingHours constraintAnnotation;

    // Test Time: 9:00
    private final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

    @BeforeEach
    void setUp() {
        validator = new ValidWorkingHoursValidator();

        when(constraintAnnotation.startTimeField()).thenReturn("workingStartTime");
        when(constraintAnnotation.endTimeField()).thenReturn("workingEndTime");

        validator.initialize(constraintAnnotation);
    }

    @ParameterizedTest(name = "Start: {0}, End: {1}, Expected: {2}")
    @CsvSource({
            "09:00, 17:00, true",
            "10:00, 09:00, true",
            "12:00, 12:00, false"
    })
    void workingHoursValidationScenarios(String startTimeStr, String endTimeStr, boolean expectedResult) {
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = LocalTime.parse(endTimeStr);

        CreateLocationRequestDTO dto = new CreateLocationRequestDTO(
                "Test Location",
                10,
                startTime,
                endTime
        );

        boolean actualResult = validator.isValid(dto, constraintValidatorContext);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    void isValid_whenEitherTimeIsNull_shouldReturnTrue() {
        CreateLocationRequestDTO dtoWithNullStart = new CreateLocationRequestDTO(
                "Null Start",
                10,
                null,
                DEFAULT_TIME
        );

        CreateLocationRequestDTO dtoWithNullEnd = new CreateLocationRequestDTO(
                "Null End",
                10,
                DEFAULT_TIME,
                null
        );

        CreateLocationRequestDTO dtoWithBothNull = new CreateLocationRequestDTO(
                "Both Null",
                10,
                null,
                null
        );

        assertThat(validator.isValid(dtoWithNullStart, constraintValidatorContext)).isTrue();
        assertThat(validator.isValid(dtoWithNullEnd, constraintValidatorContext)).isTrue();
        assertThat(validator.isValid(dtoWithBothNull, constraintValidatorContext)).isTrue();
    }
}
