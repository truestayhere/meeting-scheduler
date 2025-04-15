package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Data validation and security will be added later!

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    // Basic CRUD functionality implementation:

    // CREATE - Accepts a CreateLocationRequestDTO, returns LocationDTO
    @Transactional
    public LocationDTO createLocation(CreateLocationRequestDTO requestDTO) throws IllegalArgumentException {
        log.debug("Attempting to create location with name: {}", requestDTO.name());

        if (locationRepository.findByName(requestDTO.name()).isPresent()) {
            throw new IllegalArgumentException("Location with name " + requestDTO.name() + " already exists.");
        }

        // --- (Add later) Input Validation --

        // --- Save the Location ---

        Location newLocation = locationMapper.mapToLocation(requestDTO);

        Location savedLocation = locationRepository.save(newLocation);

        log.info("Successfully created location with ID: {}", savedLocation.getId());
        return locationMapper.mapToLocationDTO(savedLocation);
    }

    // READ - All - Returns List<LocationDTO>
    public List<LocationDTO> getAllLocations() {
        List<Location> locations = locationRepository.findAll();
        return locationMapper.mapToLocationDTOList(locations);
    }

    // READ - By ID - Accepts ID, returns LocationDTO
    public LocationDTO getLocationById(Long id) {
        Location foundLocation = findLocationEntityById(id);
        return locationMapper.mapToLocationDTO(foundLocation);
    }

    // Helper method - Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
    }

    // UPDATE - Accepts ID and UpdateLocationRequestDTO, returns LocationDTO
    @Transactional
    public LocationDTO updateLocation(Long id, UpdateLocationRequestDTO requestDTO) {
        log.debug("Attempting to update location with ID: {}", id);

        Location existingLocation = findLocationEntityById(id);

        // --- (Add later) Input Validation --

        // (Add later) Need to check if the name is unique before updating!
        existingLocation.setName(requestDTO.name());
        existingLocation.setCapacity(requestDTO.capacity());

        Location savedLocation = locationRepository.save(existingLocation);

        log.info("Successfully updated location with ID: {}", savedLocation.getId());
        return locationMapper.mapToLocationDTO(savedLocation);
    }

    // DELETE - Accepts ID
    @Transactional
    public void deleteLocation(Long id) {
        log.debug("Attempting to delete location with ID: {}", id);

        if (!locationRepository.existsById(id)) {
            throw new EntityNotFoundException("Location not found with id: " + id);
        }

        log.info("Successfully deleted location with ID: {}", id);
        locationRepository.deleteById(id);
    }

}
