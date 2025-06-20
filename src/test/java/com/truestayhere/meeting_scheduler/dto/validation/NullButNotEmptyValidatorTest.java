package com.truestayhere.meeting_scheduler.dto.validation;


import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class NullButNotEmptyValidatorTest {

    private NullButNotEmptyValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new NullButNotEmptyValidator();
    }

    private static Stream<Arguments> validationTestCases() {
        return Stream.of(
                Arguments.of(
                        "Set with items",
                        new CollectionDTO(Set.of(1L, 2L)),
                        true
                ),
                Arguments.of(
                        "List with items",
                        new CollectionDTO(List.of(1L, 2L)),
                        true
                ),
                Arguments.of(
                        "Field is null",
                        new CollectionDTO(null),
                        true
                ),
                Arguments.of(
                        "Empty Set",
                        new CollectionDTO(Set.of()),
                        false
                ),
                Arguments.of(
                        "Empty List",
                        new CollectionDTO(List.of()),
                        false
                ),
                Arguments.of(
                        "AttendeeIds Field does not exist",
                        new WithoutCollectionDTO(),
                        false
                ),
                Arguments.of(
                        "Wrong field type",
                        new WrongFieldTypeDTO("not a collection"),
                        false
                )
        );
    }

    private record CollectionDTO(
            Collection<Long> attendeeIds
    ) {
    }

    private record WithoutCollectionDTO(
    ) {
    }

    private record WrongFieldTypeDTO(
            Object attendeeIds
    ) {
    }

    @ParameterizedTest(name = "attendeeIds: {0}, Expected: {2}")
    @MethodSource("validationTestCases")
    void nullButNotEmptyValidatorScenarios(String scenario, Object dto, boolean expectedResult) {
        boolean actualResult = validator.isValid(dto, context);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

}
