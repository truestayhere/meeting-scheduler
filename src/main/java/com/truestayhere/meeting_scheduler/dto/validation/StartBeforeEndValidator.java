package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class StartBeforeEndValidator implements ConstraintValidator<StartBeforeEnd, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            // Validator assumes that there are fields startTime and endTime in value object
            // (Not the best implementation)
            var startTimeField = value.getClass().getDeclaredField("startTime");
            var endTimeField = value.getClass().getDeclaredField("endTime");
            startTimeField.setAccessible(true);
            endTimeField.setAccessible(true);

            // make private fields accessible
            Object startTimeObj = startTimeField.get(value);
            Object endTimeObj = endTimeField.get(value);

            if (startTimeObj == null || endTimeObj == null) {
                return false;
            }

            if (!(startTimeObj instanceof LocalDateTime) || !(endTimeObj instanceof LocalDateTime)) {
                return false;
            }

            // get values of start time and end time
            LocalDateTime startTime = (LocalDateTime) startTimeObj;
            LocalDateTime endTime = (LocalDateTime) endTimeObj;

            // validation logic
            return startTime.isBefore(endTime);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Error accessing startTime/endTime fields for validation: " + e.getMessage());
            return false;
        }
    }
}
