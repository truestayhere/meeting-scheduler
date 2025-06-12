package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.exception.ResourceInUseException;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
import com.truestayhere.meeting_scheduler.model.Location;
import com.truestayhere.meeting_scheduler.model.Meeting;
import com.truestayhere.meeting_scheduler.repository.LocationRepository;
import com.truestayhere.meeting_scheduler.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LocationService {
    private final LocationRepository locationRepository;
    private final MeetingRepository meetingRepository;
    private final LocationMapper locationMapper;


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

        // --- Create the Location ---

        Location newLocation = locationMapper.mapToLocation(requestDTO);

        // --- Save the Location ---

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

        String effectiveName = (requestDTO.name() != null) ? requestDTO.name() : existingLocation.getName();

        // --- Duplicates Check ---

        checkDuplicateLocation(effectiveName, id);

        // --- Update the Location ---

        locationMapper.updateLocationFromDto(requestDTO, existingLocation);

        // --- Return Updated Location ---

        log.info("Successfully updated location with ID: {}", existingLocation.getId());
        return locationMapper.mapToLocationDTO(existingLocation);
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
            throw new EntityNotFoundException("Location not found with ID: " + id);
        }

        // --- Location Occupation Check ---
        checkMeetingExistsForLocation(id);

        // --- Delete the location ---
        locationRepository.deleteById(id);
        log.info("Successfully deleted location with ID: {}", id);
    }


    // === HELPER METHODS ===


    // Accepts ID, returns Location Entity
    private Location findLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with ID: " + id));
    }


    /**
     * Checks if the specified location is associated with any meetings.
     * Throws ResourceInUseException if meetings are found.
     *
     * @param id The ID of the location to check.
     * @throws ResourceInUseException if the location is used in meetings.
     */
    private void checkMeetingExistsForLocation(Long id) {
        List<Meeting> conflictingMeetings = meetingRepository.findByLocation_id(id);
        if (!conflictingMeetings.isEmpty()) {
            List<Long> meetingIds = conflictingMeetings
                    .stream()
                    .map(Meeting::getId) // Only need meeting Ids
                    .distinct() // Remove duplicate meetings
                    .toList();
            String message = String.format("Location cannot be deleted because it is used in %d meeting(s). See details.", meetingIds.size());
            log.warn("Attempted to delete location ID: {} which is used in meetings: {}", id, meetingIds);
            throw new ResourceInUseException(message, meetingIds);
        }
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
