package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class NullButNotBlankValidatorTest {

    private NullButNotBlankValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new NullButNotBlankValidator();
    }

    private static Stream<Arguments> validationTestCases() {
        return Stream.of(
                Arguments.of(
                        "Title not blank",
                        new TitleDTO("valid title"),
                        true
                ),
                Arguments.of(
                        "Blank title",
                        new TitleDTO(""),
                        false
                ),
                Arguments.of(
                        "Field is null",
                        new TitleDTO(null),
                        true
                ),
                Arguments.of(
                        "Title field does not exist",
                        new WithoutTitleDTO(),
                        false
                ),
                Arguments.of(
                        "Wrong field type",
                        new WrongFieldTypeDTO(1L),
                        false
                )
        );
    }

    private record TitleDTO(
            String title
    ) {}

    private record WithoutTitleDTO(
    ) {}

    private record WrongFieldTypeDTO(
            Object title
    ) {}

    @ParameterizedTest(name = "attendeeIds: {0}, Expected: {2}")
    @MethodSource("validationTestCases")
    void nullButNotEmptyValidatorScenarios(String scenario, Object dto, boolean expectedResult) {
        boolean actualResult = validator.isValid(dto, context);

        assertThat(actualResult).isEqualTo(expectedResult);
    }
}
