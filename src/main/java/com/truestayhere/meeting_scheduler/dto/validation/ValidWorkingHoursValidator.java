package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalTime;

@Slf4j
public class ValidWorkingHoursValidator implements ConstraintValidator<ValidWorkingHours, Object> {
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

        if (!(startTimeObj instanceof LocalTime startTime) || !(endTimeObj instanceof LocalTime endTime)) {
            log.warn("IllegalArgumentException occurred: Fields specified in @ValidWorkingHours must be of type LocalTime");
            throw new IllegalArgumentException("Fields specified in @ValidWorkingHours must be of type LocalTime");
        }

        return !startTime.equals(endTime);

    }


}
