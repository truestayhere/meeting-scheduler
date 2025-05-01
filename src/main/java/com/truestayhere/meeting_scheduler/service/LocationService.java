package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);


    // === CRUD METHODS ===


    /**
     * Creates a location based on provided data.
     *
     * @param requestDTO A CreateLocationRequestDTO with the location data.
     * @return A LocationDTO with the created location data.
     */
    @Transactional
    public LocationDTO createLocation(CreateLocationRequestDTO requestDTO) {
        log.debug("Attempting to create location with name: {}", requestDTO.name());

        // --- Duplicates Check ---

        checkDuplicateLocation(requestDTO.name(), null);

        // --- Save the Location ---

        Location newLocation = locationMapper.mapToLocation(requestDTO);

        Location savedLocation = locationRepository.save(newLocation);

        log.info("Successfully created location with ID: {}", savedLocation.getId());
        return locationMapper.mapToLocationDTO(savedLocation);
    }


    /**
     * Fetches all locations.
     *
     * @return A list of LocationDTOs for all locations.
     */
    public List<LocationDTO> getAllLocations() {
        List<Location> locations = locationRepository.findAll();
        return locationMapper.mapToLocationDTOList(locations);
    }


    /**
     * Fetches a location based on provided ID.
     *
     * @param id The ID of the location.
     * @return A LocationDTO for the found location.
     */
    public LocationDTO getLocationById(Long id) {
        Location foundLocation = findLocationEntityById(id);
        return locationMapper.mapToLocationDTO(foundLocation);
    }


    /**
     * Updates a location based on provided ID and update data.
     *
     * @param id         The ID of the location to be updated,
     * @param requestDTO The UpdateLocationRequestDTO with the updated data.
     * @return A LocationDTO for the updated location.
     */
    @Transactional
    public LocationDTO updateLocation(Long id, UpdateLocationRequestDTO requestDTO) {
        log.debug("Attempting to update location with ID: {}", id);

        Location existingLocation = findLocationEntityById(id);

        // --- Duplicates Check ---

        checkDuplicateLocation(requestDTO.name(), id);

        // --- Save the Location ---

        existingLocation.setName(requestDTO.name());
        existingLocation.setCapacity(requestDTO.capacity());

        Location savedLocation = locationRepository.save(existingLocation);

        log.info("Successfully updated location with ID: {}", savedLocation.getId());
        return locationMapper.mapToLocationDTO(savedLocation);
    }


    /**
     * Deletes a location by ID.
     *
     * @param id The ID of the location.
     */
    @Transactional
    public void deleteLocation(Long id) {
        log.debug("Attempting to delete location with ID: {}", id);

        if (!locationRepository.existsById(id)) {
            throw new EntityNotFoundException("Location not found with id: " + id);
        }

        log.info("Successfully deleted location with ID: {}", id);
        locationRepository.deleteById(id);
    }


    // === HELPER METHODS ===


    // Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
    }

    // Location Duplicates Check - Accepts String, ID, throws IllegalArgumentException
    private void checkDuplicateLocation(String name, Long idToExclude) {

        log.debug("Checking duplicates for location with name: {}", name);

        Optional<Location> duplicateLocationOpt = locationRepository.findByName(name);

        if (duplicateLocationOpt.isPresent()) {
            Location duplicateLocation = duplicateLocationOpt.get();

            if (!duplicateLocation.getId().equals(idToExclude)) {
                String errorMessage = (idToExclude == null)
                        ? "Location with name '" + name + "' already exists."
                        : "Another location (" + duplicateLocation.getId() + ") already exists with name '" + name + "'.";
                log.warn(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }

        log.debug("No duplicates found for the provided location name.");
    }

}
