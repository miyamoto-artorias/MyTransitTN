package com.example.mytransittn.controller;

import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.service.OpenRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final StationRepository stationRepository;
    private final OpenRouteService openRouteService;

    @Autowired
    public TestController(StationRepository stationRepository, OpenRouteService openRouteService) {
        this.stationRepository = stationRepository;
        this.openRouteService = openRouteService;
    }

    @GetMapping("/distance/{stationId1}/{stationId2}")
    public ResponseEntity<?> testDistance(@PathVariable Long stationId1, @PathVariable Long stationId2) {
        Optional<Station> station1 = stationRepository.findById(stationId1);
        Optional<Station> station2 = stationRepository.findById(stationId2);
        
        if (station1.isEmpty() || station2.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "One or both stations not found"
            ));
        }
        
        try {
            double distance = openRouteService.calculateDistance(station1.get(), station2.get());
            
            Map<String, Object> result = new HashMap<>();
            result.put("station1", station1.get().getName());
            result.put("station2", station2.get().getName());
            result.put("distance_km", distance);
            result.put("coordinates1", Map.of(
                "lat", station1.get().getLatitude(),
                "lng", station1.get().getLongitude()
            ));
            result.put("coordinates2", Map.of(
                "lat", station2.get().getLatitude(), 
                "lng", station2.get().getLongitude()
            ));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to calculate distance: " + e.getMessage()
            ));
        }
    }
} 