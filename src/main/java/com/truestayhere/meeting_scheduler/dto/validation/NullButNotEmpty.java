package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NullButNotEmptyValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullButNotEmpty {

    String message() default "Attendee list cannot be empty when provided.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
