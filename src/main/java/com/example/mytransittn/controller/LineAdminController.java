package com.example.mytransittn.controller;

import com.example.mytransittn.model.Line;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.LineRepository;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/lines")
public class LineAdminController {

    private final LineRepository lineRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    @Autowired
    public LineAdminController(LineRepository lineRepository, 
                              StationRepository stationRepository,
                              UserRepository userRepository) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> createLine(@RequestBody LineRequest request) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Validate request
        if (request.getCode() == null || request.getFareMultiplier() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }
        
        // Check if line code is already in use
        if (lineRepository.findByCode(request.getCode()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Line code already exists"));
        }
        
        // Create new line
        Line line = new Line();
        line.setCode(request.getCode());
        line.setFareMultiplier(request.getFareMultiplier());
        
        // Add stations if provided
        if (request.getStationIds() != null && !request.getStationIds().isEmpty()) {
            List<Station> stations = new ArrayList<>();
            for (Long stationId : request.getStationIds()) {
                Optional<Station> station = stationRepository.findById(stationId);
                if (station.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Station with id " + stationId + " not found"));
                }
                stations.add(station.get());
            }
            line.setStations(stations);
        }
        
        Line savedLine = lineRepository.save(line);
        return ResponseEntity.ok(savedLine);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLine(@PathVariable Long id, @RequestBody LineRequest request) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Check if line exists
        Optional<Line> lineOpt = lineRepository.findById(id);
        if (lineOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Line line = lineOpt.get();
        
        // Update fields if provided
        if (request.getCode() != null) {
            // Check if another line already uses this code
            Optional<Line> existingLine = lineRepository.findByCode(request.getCode());
            if (existingLine.isPresent() && !existingLine.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Line code already exists"));
            }
            line.setCode(request.getCode());
        }
        
        if (request.getFareMultiplier() != null) {
            line.setFareMultiplier(request.getFareMultiplier());
        }
        
        // Update stations if provided
        if (request.getStationIds() != null) {
            List<Station> stations = new ArrayList<>();
            for (Long stationId : request.getStationIds()) {
                Optional<Station> station = stationRepository.findById(stationId);
                if (station.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Station with id " + stationId + " not found"));
                }
                stations.add(station.get());
            }
            line.setStations(stations);
        }
        
        Line updatedLine = lineRepository.save(line);
        return ResponseEntity.ok(updatedLine);
    }
    
    @PostMapping("/{id}/add-station")
    public ResponseEntity<?> addStationToLine(@PathVariable Long id, @RequestBody AddStationRequest request) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Validate request
        if (request.getStationId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Station ID is required"));
        }
        
        // Check if line exists
        Optional<Line> lineOpt = lineRepository.findById(id);
        if (lineOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if station exists
        Optional<Station> stationOpt = stationRepository.findById(request.getStationId());
        if (stationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Station not found"));
        }
        
        Line line = lineOpt.get();
        Station station = stationOpt.get();
        
        // Add station to line
        List<Station> stations = new ArrayList<>(line.getStations());
        if (stations.contains(station)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Station already in this line"));
        }
        
        // Add at position or at the end
        if (request.getPosition() != null && request.getPosition() >= 0 && request.getPosition() <= stations.size()) {
            stations.add(request.getPosition(), station);
        } else {
            stations.add(station);
        }
        
        line.setStations(stations);
        Line updatedLine = lineRepository.save(line);
        
        return ResponseEntity.ok(updatedLine);
    }
    
    @PostMapping("/{id}/remove-station")
    public ResponseEntity<?> removeStationFromLine(@PathVariable Long id, @RequestBody RemoveStationRequest request) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Validate request
        if (request.getStationId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Station ID is required"));
        }
        
        // Check if line exists
        Optional<Line> lineOpt = lineRepository.findById(id);
        if (lineOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if station exists
        Optional<Station> stationOpt = stationRepository.findById(request.getStationId());
        if (stationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Station not found"));
        }
        
        Line line = lineOpt.get();
        Station station = stationOpt.get();
        
        // Remove station from line
        List<Station> stations = new ArrayList<>(line.getStations());
        if (!stations.contains(station)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Station not in this line"));
        }
        
        stations.remove(station);
        line.setStations(stations);
        Line updatedLine = lineRepository.save(line);
        
        return ResponseEntity.ok(updatedLine);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLine(@PathVariable Long id) {
        // Check admin authorization
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        // Check if line exists
        if (!lineRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        lineRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Line deleted successfully"));
    }
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
    
    // Request DTOs
    static class LineRequest {
        private String code;
        private BigDecimal fareMultiplier;
        private List<Long> stationIds;
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public BigDecimal getFareMultiplier() {
            return fareMultiplier;
        }
        
        public void setFareMultiplier(BigDecimal fareMultiplier) {
            this.fareMultiplier = fareMultiplier;
        }
        
        public List<Long> getStationIds() {
            return stationIds;
        }
        
        public void setStationIds(List<Long> stationIds) {
            this.stationIds = stationIds;
        }
    }
    
    static class AddStationRequest {
        private Long stationId;
        private Integer position;
        
        public Long getStationId() {
            return stationId;
        }
        
        public void setStationId(Long stationId) {
            this.stationId = stationId;
        }
        
        public Integer getPosition() {
            return position;
        }
        
        public void setPosition(Integer position) {
            this.position = position;
        }
    }
    
    static class RemoveStationRequest {
        private Long stationId;
        
        public Long getStationId() {
            return stationId;
        }
        
        public void setStationId(Long stationId) {
            this.stationId = stationId;
        }
    }
} 