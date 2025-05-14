package com.example.mytransittn.controller;

import com.example.mytransittn.dto.PathResultDto;
import com.example.mytransittn.dto.StationDto;
import com.example.mytransittn.model.PathResult;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<List<StationDto>> getAllStations() {
        List<StationDto> stationDtos = stationRepository.findAll().stream()
                .map(StationDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stationDtos);
    }

    @GetMapping("/stations/open")
    public ResponseEntity<List<StationDto>> getOpenStations() {
        List<StationDto> stationDtos = stationRepository.findAllOpenStations().stream()
                .map(StationDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stationDtos);
    }

    @GetMapping("/stations/{id}")
    public ResponseEntity<StationDto> getStationById(@PathVariable Long id) {
        return stationRepository.findById(id)
                .map(StationDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/route")
    public ResponseEntity<PathResultDto> findRoute(@RequestParam Long from, @RequestParam Long to) {
        try {
            PathResult result = navigationService.findShortestPath(from, to);
            PathResultDto resultDto = PathResultDto.fromEntity(result);
            return ResponseEntity.ok(resultDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stations/by-state/{stateId}")
    public ResponseEntity<List<StationDto>> getStationsByState(@PathVariable Long stateId) {
        List<StationDto> stationDtos = stationRepository.findByStateId(stateId).stream()
                .map(StationDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stationDtos);
    }

    @GetMapping("/stations/by-line/{lineId}")
    public ResponseEntity<List<StationDto>> getStationsByLine(@PathVariable Long lineId) {
        List<StationDto> stationDtos = stationRepository.findStationsByLineIdOrdered(lineId).stream()
                .map(StationDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stationDtos);
    }
} 