package com.truestayhere.meeting_scheduler.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.util.Collection;

@Slf4j
public class NullButNotEmptyValidator implements ConstraintValidator<NullButNotEmpty, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

            Object attendeeIdsObj = beanWrapper.getPropertyValue("attendeeIds");

            if (attendeeIdsObj == null) {
                return true;
            }

            if (!(attendeeIdsObj instanceof Collection<?> attendeeIds)) {
                log.error("attendeeIds field is not of type Collection for object: {}", value);
                return false;
            }

            // validation logic
            return !attendeeIds.isEmpty();

        } catch (BeansException e) {
            log.error("Unexpected error during @NullButNotEmpty validation on object {}: {}", value, e.getMessage(), e);
            return false;
        }
    }

}
