package com.truestayhere.meeting_scheduler.dto.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented // Include in JavaDoc
@Constraint(validatedBy = StartBeforeEndValidator.class) // Specifies the class with validation logic
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE}) // This annotation can be placed on class/interface/record...
@Retention(RetentionPolicy.RUNTIME) // Ensures that the annotation is available to JVM at runtime
public @interface StartBeforeEnd { // @interface makes it and Annotation

    // message(), groups(), payload(): Standard elements required by Bean Validation specification.

    // Default error massage
    String message() default "Start time must me before end time";

    // Groups allow subset validation
    Class<?>[] groups() default {};

    // Payload allows attaching metadata
    Class<? extends Payload>[] payload() default {};

}
