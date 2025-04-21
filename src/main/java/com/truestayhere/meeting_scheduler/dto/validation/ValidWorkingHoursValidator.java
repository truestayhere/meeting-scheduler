package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalTime;

public class ValidWorkingHoursValidator implements ConstraintValidator<ValidWorkingHours, Object> {
    private static final Logger log = LoggerFactory.getLogger(ValidWorkingHoursValidator.class);

    private String startTimeFieldName;
    private String endTimeFieldName;

    @Override
    public void initialize(ValidWorkingHours constraintAnnotation) {
        this.startTimeFieldName = constraintAnnotation.startTimeField();
        this.endTimeFieldName = constraintAnnotation.endTimeField();
    }


    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

        Object startTimeObj = beanWrapper.getPropertyValue(startTimeFieldName);
        Object endTimeObj = beanWrapper.getPropertyValue(endTimeFieldName);

        if (startTimeObj == null || endTimeObj == null) {
            return true;
        }

        if (!(startTimeObj instanceof LocalTime) || !(endTimeObj instanceof LocalTime)) {
            log.warn("IllegalArgumentException occurred: Fields specified in @ValidWorkingHours must be of type LocalTime");
            throw new IllegalArgumentException("Fields specified in @ValidWorkingHours must be of type LocalTime");
        }

        LocalTime startTime = (LocalTime) startTimeObj;
        LocalTime endTime = (LocalTime) endTimeObj;

        return !startTime.equals(endTime);

    }


}
