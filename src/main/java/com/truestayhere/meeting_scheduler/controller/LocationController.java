package com.truestayhere.meeting_scheduler.controller;


import com.truestayhere.meeting_scheduler.dto.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    // GET /api/locations - Get all locations
    @GetMapping
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        List<LocationDTO> locations = locationService.getAllLocations();
        return ResponseEntity.ok(locations); // 200 OK
    }

    // GET /api/locations/is - Get location by ID
    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable Long id) {
        LocationDTO location = locationService.getLocationById(id);
        return ResponseEntity.ok(location); // 200 OK
    }

    // POST /api/locations - Create a new location
    @PostMapping
    public ResponseEntity<LocationDTO> createLocation(@Valid @RequestBody CreateLocationRequestDTO requestDTO) {
        LocationDTO createdLocation = locationService.createLocation(requestDTO);
        return new ResponseEntity<>(createdLocation, HttpStatus.CREATED); // 201 CREATED
    }

    // PUT /api/locations/id - Update location by ID
    @PutMapping("/{id}")
    public ResponseEntity<LocationDTO> updateLocationById(@PathVariable Long id, @Valid @RequestBody UpdateLocationRequestDTO requestDTO) {
        LocationDTO updatedLocation = locationService.updateLocation(id, requestDTO);
        return ResponseEntity.ok(updatedLocation); // 200 OK
    }

    // DELETE /api/locations/id - Delete location by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocationById(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build(); // 204 NO CONTENT
    }


}
