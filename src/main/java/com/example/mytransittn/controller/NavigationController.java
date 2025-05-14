package com.example.mytransittn.controller;

import com.example.mytransittn.model.PathResult;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationService navigationService;
    private final StationRepository stationRepository;

    @Autowired
    public NavigationController(NavigationService navigationService, StationRepository stationRepository) {
        this.navigationService = navigationService;
        this.stationRepository = stationRepository;
    }

    @GetMapping("/stations")
    public ResponseEntity<List<Station>> getAllStations() {
        return ResponseEntity.ok(stationRepository.findAll());
    }

    @GetMapping("/stations/open")
    public ResponseEntity<List<Station>> getOpenStations() {
        return ResponseEntity.ok(stationRepository.findAllOpenStations());
    }

    @GetMapping("/stations/{id}")
    public ResponseEntity<Station> getStationById(@PathVariable Long id) {
        return stationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/route")
    public ResponseEntity<PathResult> findRoute(@RequestParam Long from, @RequestParam Long to) {
        try {
            PathResult result = navigationService.findShortestPath(from, to);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stations/by-state/{stateId}")
    public ResponseEntity<List<Station>> getStationsByState(@PathVariable Long stateId) {
        return ResponseEntity.ok(stationRepository.findByStateId(stateId));
    }

    @GetMapping("/stations/by-line/{lineId}")
    public ResponseEntity<List<Station>> getStationsByLine(@PathVariable Long lineId) {
        return ResponseEntity.ok(stationRepository.findStationsByLineIdOrdered(lineId));
    }
} 