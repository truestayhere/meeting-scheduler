package com.truestayhere.meeting_scheduler.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Role {
    ADMIN("ADMIN"),
    USER("USER");

    private final String value;

    @JsonValue
    public String toJson() {
        return value;
    }

    @JsonCreator
    public static Role fromString(String value) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
