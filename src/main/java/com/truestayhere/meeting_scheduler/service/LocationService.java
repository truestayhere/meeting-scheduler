package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Data validation and security will be added later!

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locationRepository;

    // Basic CRUD functionality implementation:

    // CREATE
    @Transactional
    public Location addLocation(Location location) throws IllegalAccessException {
        if (locationRepository.findByName(location.getName()).isPresent()) {
            throw new IllegalAccessException("Location with name " + location.getName() + " already exists.");
        }
        return locationRepository.save(location);
    }

    // READ - All
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    // READ - By ID
    public Location getLocationById(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
    }

    // UPDATE
    public Location updateLocation(Long id, Location locationDetails) {
        Location existingLocation = getLocationById(id);

        // (Add later) Need to check if the name is unique before updating!
        existingLocation.setName(locationDetails.getName());
        existingLocation.setCapacity(locationDetails.getCapacity());

        return locationRepository.save(existingLocation);
    }

    // DELETE
    @Transactional
    public void deleteLocation(Long id) {
        Location existingLocation = getLocationById(id);
        locationRepository.delete(existingLocation);
    }
}
