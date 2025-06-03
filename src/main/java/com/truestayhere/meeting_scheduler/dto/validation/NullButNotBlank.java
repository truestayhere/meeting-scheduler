package com.truestayhere.meeting_scheduler.dto.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NullButNotBlankValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullButNotBlank {

    String message() default "Title cannot be blank when provided.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
