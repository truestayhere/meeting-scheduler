package com.truestayhere.meeting_scheduler.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.CONFLICT)
@Getter
public class ResourceInUseException extends RuntimeException {

    private final List<Long> conflictingResourceIds;

    public ResourceInUseException(String message, List<Long> conflictingResourceIds) {
        super(message);
        this.conflictingResourceIds = (conflictingResourceIds != null) ? conflictingResourceIds : List.of();
    }

    public ResourceInUseException(String message) {
        super(message);
        this.conflictingResourceIds = List.of();
    }
}
