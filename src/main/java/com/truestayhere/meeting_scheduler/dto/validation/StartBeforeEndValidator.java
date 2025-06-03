package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.time.LocalDateTime;

@Slf4j
public class StartBeforeEndValidator implements ConstraintValidator<StartBeforeEnd, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

            // make private fields accessible
            Object startTimeObj = beanWrapper.getPropertyValue("startTime");
            Object endTimeObj = beanWrapper.getPropertyValue("endTime");

            if (startTimeObj == null || endTimeObj == null) {
                return true;
            }

            if (!(startTimeObj instanceof LocalDateTime startTime) || !(endTimeObj instanceof LocalDateTime endTime)) {
                log.error("startTime or endTime fields are not of type LocalDateTime for object: {}", value);
                return false;
            }

            // get values of start time and end time

            // validation logic
            return startTime.isBefore(endTime);

        } catch (BeansException e) {
            log.error("Unexpected error during @StartBeforeEnd validation on object {}: {}", value, e.getMessage(), e);
            return false;
        }
    }
}
