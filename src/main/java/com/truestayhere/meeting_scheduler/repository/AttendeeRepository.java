package com.truestayhere.meeting_scheduler.repository;

import com.truestayhere.meeting_scheduler.model.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    // <Attendee, Long> means that Entity type - Attendee, PK type - Long

    // Spring JPA inherits basic CRUD methods automatically

    // (derived query) Spring JPA generates SELECT query based on the method name "findBy[variable name]"
    // Find attendee by email
    // Example SQL Query:
    // SELECT a.id, a.name, a.email FROM attendee a WHERE a.email = ? LIMIT 1;
    Optional<Attendee> findByEmail(String email);
    // "Optional" means that the method can return null if nothing was found
    // Basically helps to avoid NullPointerExceptions
}
