package com.truestayhere.meeting_scheduler.service;

import com.truestayhere.meeting_scheduler.dto.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.mapper.LocationMapper;
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
    private final LocationMapper locationMapper;

    // Basic CRUD functionality implementation:

    // CREATE - Accepts a CreateLocationRequestDTO, returns LocationDTO
    @Transactional
    public LocationDTO addLocation(CreateLocationRequestDTO requestDTO) throws IllegalAccessException {
        if (locationRepository.findByName(requestDTO.name()).isPresent()) {
            throw new IllegalAccessException("Location with name " + requestDTO.name() + " already exists.");
        }

        // --- (Add later) Input Validation --

        // --- Save the Location ---

        Location newLocation = locationMapper.mapToLocation(requestDTO);

        Location savedLocation = locationRepository.save(newLocation);
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
    public LocationDTO updateLocation(Long id, UpdateLocationRequestDTO requestDTO) {
        Location existingLocation = findLocationEntityById(id);

        // --- (Add later) Input Validation --

        // (Add later) Need to check if the name is unique before updating!
        existingLocation.setName(requestDTO.name());
        existingLocation.setCapacity(requestDTO.capacity());

        Location savedLocation = locationRepository.save(existingLocation);
        return locationMapper.mapToLocationDTO(savedLocation);
    }

    // DELETE - Accepts ID
    @Transactional
    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new EntityNotFoundException("Location not found with id: " + id);
        }
        locationRepository.deleteById(id);
    }

}
