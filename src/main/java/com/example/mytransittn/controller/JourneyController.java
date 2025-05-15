package com.example.mytransittn.controller;

import com.example.mytransittn.dto.JourneyDto;
import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.JourneyRepository;
import com.example.mytransittn.repository.LineRepository;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.repository.UserRepository;
import com.example.mytransittn.service.FareCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final LineRepository lineRepository;
    private final FareCalculationService fareCalculationService;

    @Autowired
    public JourneyController(JourneyRepository journeyRepository, UserRepository userRepository,
                            StationRepository stationRepository, LineRepository lineRepository,
                            FareCalculationService fareCalculationService) {
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.lineRepository = lineRepository;
        this.fareCalculationService = fareCalculationService;
    }

    @GetMapping
    public ResponseEntity<List<JourneyDto>> getUserJourneys() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<JourneyDto> journeyDtos = journeyRepository.findByUserOrderByStartTimeDesc(user)
            .stream()
            .map(JourneyDto::fromEntity)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(journeyDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JourneyDto> getJourneyById(@PathVariable Long id) {
        Optional<Journey> journey = journeyRepository.findById(id);
        
        if (journey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the journey belongs to the current user or user is admin
        User currentUser = getCurrentUser();
        if (currentUser == null || (!journey.get().getUser().equals(currentUser) && !currentUser.isAdmin())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(JourneyDto.fromEntity(journey.get()));
    }

    @PostMapping
    public ResponseEntity<?> createJourney(@RequestBody JourneyDto.JourneyRequestDto request) {
        // Get the raw authentication - don't check properties yet
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication details: " + 
            (auth != null ? 
             "Name: " + auth.getName() + 
             ", Principal: " + auth.getPrincipal() + 
             ", Authenticated: " + auth.isAuthenticated() +
             ", Authorities: " + auth.getAuthorities() 
             : "null"));
        
        // Check if user is authenticated
        if (auth == null || !auth.isAuthenticated() || 
            "anonymousUser".equals(auth.getPrincipal().toString())) {
            System.out.println("User not authenticated, returning 401");
            return ResponseEntity.status(401)
                .body(Map.of("error", "User not authenticated", 
                             "message", "Please use the Authorize button in Swagger UI and enter your JWT token with Bearer prefix"));
        }

        // Try to get current user from repository
        User user = userRepository.findByEmail(auth.getName())
            .orElse(null);
        
        if (user == null) {
            System.out.println("User not found in database despite authentication: " + auth.getName());
            return ResponseEntity.status(401)
                .body(Map.of("error", "User not found", 
                             "message", "Your authentication was accepted but user data could not be found"));
        }
        
        System.out.println("User authenticated: " + user.getUsername() + " (ID: " + user.getId() + ")");

        // Validate request
        if (request.getStartStationId() == null || request.getEndStationId() == null || 
            request.getLineId() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing required fields"));
        }

        // Find stations and line
        var startStationOpt = stationRepository.findById(request.getStartStationId());
        var endStationOpt = stationRepository.findById(request.getEndStationId());
        var lineOpt = lineRepository.findById(request.getLineId());

        // Validate entities exist
        if (startStationOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Start station not found (ID: " + request.getStartStationId() + ")"));
        }
        
        if (endStationOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "End station not found (ID: " + request.getEndStationId() + ")"));
        }
        
        if (lineOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Line not found (ID: " + request.getLineId() + ")"));
        }
        
        var startStation = startStationOpt.get();
        var endStation = endStationOpt.get();
        var line = lineOpt.get();
        
        // Validate that both stations are on the selected line
        boolean startOnLine = false;
        boolean endOnLine = false;
        int startIndex = -1;
        int endIndex = -1;
        
        List<Station> lineStations = line.getStations();
        for (int i = 0; i < lineStations.size(); i++) {
            if (lineStations.get(i).getId().equals(startStation.getId())) {
                startOnLine = true;
                startIndex = i;
            }
            if (lineStations.get(i).getId().equals(endStation.getId())) {
                endOnLine = true;
                endIndex = i;
            }
        }
        
        if (!startOnLine) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Start station is not on the selected line"));
        }
        
        if (!endOnLine) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "End station is not on the selected line"));
        }
        
        // Create journey
        Journey journey = new Journey();
        journey.setStartStation(startStation);
        journey.setEndStation(endStation);
        journey.setLine(line);
        journey.setUser(user);
        journey.setStartTime(LocalDateTime.now());
        journey.setStatus(Journey.JourneyStatus.PLANNED);
        
        // Calculate distance for the journey
        try {
            double distance = fareCalculationService.computeDistance(
                startStation, endStation, journey);
            journey.setDistanceKm(distance);
            
            // Pre-calculate fare for informational purposes
            // Note: This will be recalculated when journey is completed
            BigDecimal fare = fareCalculationService.calculateFare(journey);
            journey.setFare(fare);
        } catch (Exception e) {
            System.err.println("Failed to pre-calculate fare/distance: " + e.getMessage());
            // Continue even if calculation fails
        }

        Journey savedJourney = journeyRepository.save(journey);
        return ResponseEntity.ok(JourneyDto.fromEntity(savedJourney));
    }

    @PutMapping("/{id}/start")
    @Transactional
    public ResponseEntity<JourneyDto> startJourney(@PathVariable Long id) {
        Optional<Journey> journeyOpt = journeyRepository.findById(id);
        
        if (journeyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Journey journey = journeyOpt.get();
        User currentUser = getCurrentUser();
        
        if (currentUser == null || !journey.getUser().equals(currentUser)) {
            return ResponseEntity.status(403).build();
        }
        
        // Only PURCHASED journeys can be started
        if (journey.getStatus() != Journey.JourneyStatus.PURCHASED) {
            return ResponseEntity.badRequest()
                .body(createErrorDto("Only purchased journeys can be started. Please pay for the journey first."));
        }
        
        journey.setStartTime(LocalDateTime.now()); // Update start time to now
        
        Journey updatedJourney = journeyRepository.save(journey);
        return ResponseEntity.ok(JourneyDto.fromEntity(updatedJourney));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<JourneyDto> completeJourney(@PathVariable Long id) {
        try {
            // Find the journey
            Optional<Journey> journeyOpt = journeyRepository.findById(id);
            if (journeyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Journey journey = journeyOpt.get();
            User currentUser = getCurrentUser();
            
            // Check authorization
            if (currentUser == null || !journey.getUser().equals(currentUser)) {
                return ResponseEntity.status(403).build();
            }
            
            // Check status - now only PURCHASED journeys can be completed
            if (journey.getStatus() != Journey.JourneyStatus.PURCHASED) {
                return ResponseEntity.badRequest()
                    .body(createErrorDto("Only purchased journeys can be completed."));
            }
            
            // Update journey state
            journey.setStatus(Journey.JourneyStatus.COMPLETED);
            journey.setEndTime(LocalDateTime.now());
            
            // Calculate the distance - with fallback mechanism
            double distance;
            try {
                distance = fareCalculationService.computeDistance(
                    journey.getStartStation(), journey.getEndStation(), journey);
            } catch (Exception e) {
                System.err.println("Error calculating distance: " + e.getMessage());
                // Default to a reasonable minimum distance if calculation fails
                distance = 1.0; // 1 km minimum
            }
            journey.setDistanceKm(distance);
            
            // The fare has already been calculated and paid for
            
            // Save the updated journey
            Journey updatedJourney = journeyRepository.save(journey);
            return ResponseEntity.ok(JourneyDto.fromEntity(updatedJourney));
        } catch (Exception e) {
            e.printStackTrace();
            JourneyDto errorDto = new JourneyDto();
            errorDto.setError("Failed to complete journey: " + e.getMessage());
            return ResponseEntity.status(500).body(errorDto);
        }
    }

    @PutMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<JourneyDto> cancelJourney(@PathVariable Long id) {
        Optional<Journey> journeyOpt = journeyRepository.findById(id);
        
        if (journeyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Journey journey = journeyOpt.get();
        User currentUser = getCurrentUser();
        
        if (currentUser == null || !journey.getUser().equals(currentUser)) {
            return ResponseEntity.status(403).build();
        }
        
        // Only journeys that aren't already COMPLETED or CANCELLED can be cancelled
        if (journey.getStatus() == Journey.JourneyStatus.COMPLETED || 
            journey.getStatus() == Journey.JourneyStatus.CANCELLED) {
            return ResponseEntity.badRequest().build();
        }
        
        journey.setStatus(Journey.JourneyStatus.CANCELLED);
        Journey updatedJourney = journeyRepository.save(journey);
        return ResponseEntity.ok(JourneyDto.fromEntity(updatedJourney));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JourneyDto>> getJourneysByStatus(@PathVariable String status) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Journey.JourneyStatus journeyStatus = Journey.JourneyStatus.valueOf(status.toUpperCase());
            List<JourneyDto> journeyDtos = journeyRepository.findByUserAndStatus(user, journeyStatus)
                .stream()
                .map(JourneyDto::fromEntity)
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(journeyDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/between")
    public ResponseEntity<List<JourneyDto>> getJourneysBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        List<JourneyDto> journeyDtos = journeyRepository.findJourneysBetween(start, end)
            .stream()
            .map(JourneyDto::fromEntity)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(journeyDtos);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // Helper method to create error DTOs
    private JourneyDto createErrorDto(String errorMessage) {
        JourneyDto errorDto = new JourneyDto();
        errorDto.setError(errorMessage);
        return errorDto;
    }
} 