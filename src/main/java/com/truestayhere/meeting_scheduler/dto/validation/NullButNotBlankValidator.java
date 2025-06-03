package com.truestayhere.meeting_scheduler.dto.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;


@Slf4j
public class NullButNotBlankValidator implements ConstraintValidator<NullButNotBlank, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

            Object titleObj = beanWrapper.getPropertyValue("title");

            if (titleObj == null) {
                return true;
            }

            if (!(titleObj instanceof String title)) {
                log.error("title field is not of type String for object: {}", value);
                return false;
            }

            // validation logic
            return !title.isBlank();

        } catch (BeansException e) {
            log.error("Unexpected error during @NullButNotBlank validation on object {}: {}", value, e.getMessage(), e);
            return false;
        }
    }
}
