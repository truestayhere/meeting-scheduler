package com.truestayhere.meeting_scheduler.mapper;

import com.truestayhere.meeting_scheduler.dto.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateLocationRequestDTO;
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

        return location;
    }

    // Map from UpdateLocationRequestDTO to Location Entity
    public Location mapToLocation(UpdateLocationRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }
        Location location = new Location();

        location.setName(requestDTO.name());
        location.setCapacity(requestDTO.capacity());

        return location;
    }
}
