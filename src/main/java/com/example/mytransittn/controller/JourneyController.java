package com.example.mytransittn.controller;

import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.JourneyRepository;
import com.example.mytransittn.repository.LineRepository;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final LineRepository lineRepository;

    @Autowired
    public JourneyController(JourneyRepository journeyRepository, UserRepository userRepository,
                            StationRepository stationRepository, LineRepository lineRepository) {
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.lineRepository = lineRepository;
    }

    @GetMapping
    public ResponseEntity<List<Journey>> getUserJourneys() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(journeyRepository.findByUserOrderByStartTimeDesc(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Journey> getJourneyById(@PathVariable Long id) {
        Optional<Journey> journey = journeyRepository.findById(id);
        
        if (journey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the journey belongs to the current user or user is admin
        User currentUser = getCurrentUser();
        if (currentUser == null || (!journey.get().getUser().equals(currentUser) && !currentUser.isAdmin())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(journey.get());
    }

    @PostMapping
    public ResponseEntity<Journey> createJourney(@RequestBody JourneyRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        // Validate request
        if (request.getStartStationId() == null || request.getEndStationId() == null || 
            request.getLineId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Find stations and line
        var startStation = stationRepository.findById(request.getStartStationId());
        var endStation = stationRepository.findById(request.getEndStationId());
        var line = lineRepository.findById(request.getLineId());

        if (startStation.isEmpty() || endStation.isEmpty() || line.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Create journey
        Journey journey = new Journey();
        journey.setStartStation(startStation.get());
        journey.setEndStation(endStation.get());
        journey.setLine(line.get());
        journey.setUser(user);
        journey.setStartTime(LocalDateTime.now());
        journey.setStatus(Journey.JourneyStatus.PLANNED);

        return ResponseEntity.ok(journeyRepository.save(journey));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<Journey> startJourney(@PathVariable Long id) {
        return updateJourneyStatus(id, Journey.JourneyStatus.IN_PROGRESS);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Journey> completeJourney(@PathVariable Long id) {
        ResponseEntity<Journey> response = updateJourneyStatus(id, Journey.JourneyStatus.COMPLETED);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            Journey journey = response.getBody();
            if (journey != null) {
                journey.setEndTime(LocalDateTime.now());
                journeyRepository.save(journey);
            }
        }
        
        return response;
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Journey> cancelJourney(@PathVariable Long id) {
        return updateJourneyStatus(id, Journey.JourneyStatus.CANCELLED);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Journey>> getJourneysByStatus(@PathVariable String status) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Journey.JourneyStatus journeyStatus = Journey.JourneyStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(journeyRepository.findByUserAndStatus(user, journeyStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/between")
    public ResponseEntity<List<Journey>> getJourneysBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(journeyRepository.findJourneysBetween(start, end));
    }

    private ResponseEntity<Journey> updateJourneyStatus(Long id, Journey.JourneyStatus status) {
        Optional<Journey> journeyOpt = journeyRepository.findById(id);
        
        if (journeyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Journey journey = journeyOpt.get();
        User currentUser = getCurrentUser();
        
        if (currentUser == null || !journey.getUser().equals(currentUser)) {
            return ResponseEntity.status(403).build();
        }
        
        journey.setStatus(status);
        return ResponseEntity.ok(journeyRepository.save(journey));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // Request DTO for journey creation
    static class JourneyRequest {
        private Long startStationId;
        private Long endStationId;
        private Long lineId;

        public Long getStartStationId() {
            return startStationId;
        }

        public void setStartStationId(Long startStationId) {
            this.startStationId = startStationId;
        }

        public Long getEndStationId() {
            return endStationId;
        }

        public void setEndStationId(Long endStationId) {
            this.endStationId = endStationId;
        }

        public Long getLineId() {
            return lineId;
        }

        public void setLineId(Long lineId) {
            this.lineId = lineId;
        }
    }
} 