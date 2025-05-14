package com.example.mytransittn.controller;

import com.example.mytransittn.model.Station;
import com.example.mytransittn.model.State;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.StateRepository;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/stations")
public class StationAdminController {

    private final StationRepository stationRepository;
    private final StateRepository stateRepository;
    private final UserRepository userRepository;

    @Autowired
    public StationAdminController(StationRepository stationRepository, 
                                 StateRepository stateRepository,
                                 UserRepository userRepository) {
        this.stationRepository = stationRepository;
        this.stateRepository = stateRepository;
        this.userRepository = userRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> createStation(@RequestBody StationRequest request) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Validate request
        if (request.getName() == null || request.getLatitude() == null || 
            request.getLongitude() == null || request.getStateId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }
        
        // Check if state exists
        Optional<State> state = stateRepository.findById(request.getStateId());
        if (state.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "State not found"));
        }
        
        // Create new station
        Station station = new Station();
        station.setName(request.getName());
        station.setLatitude(request.getLatitude());
        station.setLongitude(request.getLongitude());
        station.setState(state.get());
        station.setStatus(request.getStatus() != null ? 
                request.getStatus() : Station.StationStatus.OPEN);
        
        Station savedStation = stationRepository.save(station);
        return ResponseEntity.ok(savedStation);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStation(@PathVariable Long id, @RequestBody StationRequest request) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Check if station exists
        Optional<Station> stationOpt = stationRepository.findById(id);
        if (stationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Station station = stationOpt.get();
        
        // Update fields if provided
        if (request.getName() != null) {
            station.setName(request.getName());
        }
        
        if (request.getLatitude() != null) {
            station.setLatitude(request.getLatitude());
        }
        
        if (request.getLongitude() != null) {
            station.setLongitude(request.getLongitude());
        }
        
        if (request.getStateId() != null) {
            Optional<State> state = stateRepository.findById(request.getStateId());
            if (state.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "State not found"));
            }
            station.setState(state.get());
        }
        
        if (request.getStatus() != null) {
            station.setStatus(request.getStatus());
        }
        
        Station updatedStation = stationRepository.save(station);
        return ResponseEntity.ok(updatedStation);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStation(@PathVariable Long id) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Check if station exists
        if (!stationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        stationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Station deleted successfully"));
    }
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
    
    // Request DTO for station creation/update
    static class StationRequest {
        private String name;
        private Double latitude;
        private Double longitude;
        private Long stateId;
        private Station.StationStatus status;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Double getLatitude() {
            return latitude;
        }
        
        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }
        
        public Double getLongitude() {
            return longitude;
        }
        
        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
        
        public Long getStateId() {
            return stateId;
        }
        
        public void setStateId(Long stateId) {
            this.stateId = stateId;
        }
        
        public Station.StationStatus getStatus() {
            return status;
        }
        
        public void setStatus(Station.StationStatus status) {
            this.status = status;
        }
    }
} 