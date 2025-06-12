package com.truestayhere.meeting_scheduler.controller;


import com.truestayhere.meeting_scheduler.dto.request.CreateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.LocationAvailabilityRequestDTO;
import com.truestayhere.meeting_scheduler.dto.request.UpdateLocationRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.AvailableSlotDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationDTO;
import com.truestayhere.meeting_scheduler.dto.response.LocationTimeSlotDTO;
import com.truestayhere.meeting_scheduler.service.AvailabilityService;
import com.truestayhere.meeting_scheduler.service.LocationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LocationController {
    private final LocationService locationService;
    private final AvailabilityService availabilityService;

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

    // GET /api/locations/id/availability?date=YYYY-MM-DD - Get location available time slots for a specific date
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<AvailableSlotDTO>> getLocationAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AvailableSlotDTO> availableSlots = availabilityService.getAvailableTimeForLocation(id, date);
        return ResponseEntity.ok(availableSlots);
    }


    // POST /api/locations/availability-by-duration - Find locations with sufficient time gaps on a specific date
    @PostMapping("/availability-by-duration")
    public ResponseEntity<List<LocationTimeSlotDTO>> findLocationAvailabilityByDuration(
            @Valid @RequestBody LocationAvailabilityRequestDTO requestDTO) {
        List<LocationTimeSlotDTO> locationSlots = availabilityService.getAvailabilityForLocationsByDuration(requestDTO);
        return ResponseEntity.ok(locationSlots); // 200 OK
    }


}
