package com.truestayhere.meeting_scheduler.repository;

import com.truestayhere.meeting_scheduler.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    // Find a location by name
    // Example SQL Query:
    // SELECT l.id, l.name, l.capacity FROM location l WHERE l.name = ? LIMIT 1;
    Optional<Location> findByName(String name);

    // More queries will be added later
}
