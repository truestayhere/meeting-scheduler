package com.truestayhere.meeting_scheduler.exception;


public class MeetingConflictException extends RuntimeException { // make it unchecked exception

    public MeetingConflictException(String message) {
        super(message);
    }

    public MeetingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
