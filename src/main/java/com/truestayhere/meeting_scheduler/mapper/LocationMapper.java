package com.truestayhere.meeting_scheduler.mapper;

import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.model.Location;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class LocationMapper {
    // DTO - Entity mapping methods

    // Map from Location Entity to LocationDTO
    public LocationDTO mapToLocationDTO(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationDTO(
                location.getId(),
                location.getName(),
                location.getCapacity()
        );
    }

    // Map from List<Location> to List<LocationDTO>
    public List<LocationDTO> mapToLocationDTOList(List<Location> locations) {
        if (locations == null) {
            return List.of(); // Returns empty list
        }
        return locations.stream()
                .map(this::mapToLocationDTO)
                .collect(Collectors.toList());
    }

    // Map from Set<Location> to Set<LocationDTO>
    public Set<LocationDTO> mapToLocationDTOSet(Set<Location> locations) {
        if (locations == null) {
            return Set.of();
        }
        return locations.stream()
                .map(this::mapToLocationDTO)
                .collect(Collectors.toSet());
    }

    // Map from CreateLocationRequestDTO to Location Entity
    public Location mapToLocation(CreateLocationRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }
        Location location = new Location();

        location.setName(requestDTO.name());
        location.setCapacity(requestDTO.capacity());

        // Default working start time 9:00
        location.setWorkingStartTime(requestDTO.workingStartTime());

        // Default working end time 17:00
        location.setWorkingEndTime(requestDTO.workingEndTime());

        return location;
    }

    // Map from UpdateLocationRequestDTO to Location Entity
    public void updateLocationFromDto(UpdateLocationRequestDTO requestDTO, Location location) {
        if (requestDTO.name() != null) {
            location.setName(requestDTO.name());
        }
        if (requestDTO.capacity() != null) {
            location.setCapacity(requestDTO.capacity());
        }
        if (requestDTO.workingStartTime() != null) {
            location.setWorkingStartTime(requestDTO.workingStartTime());
        }
        if (requestDTO.workingEndTime() != null) {
            location.setWorkingEndTime(requestDTO.workingEndTime());
        }
    }
}
