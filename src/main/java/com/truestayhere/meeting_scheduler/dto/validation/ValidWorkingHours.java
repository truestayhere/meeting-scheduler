package com.truestayhere.meeting_scheduler.dto.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidWorkingHoursValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWorkingHours {

    String message() default "Working start time and end time cannot be the same";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // Field names
    String startTimeField() default "workingStartTime";

    String endTimeField() default "workingEndTime";
}
