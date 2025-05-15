package com.example.mytransittn.service;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.Line;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.FareConfigurationRepository;
import com.example.mytransittn.repository.LineRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class FareCalculationService {
    private final FareConfigurationRepository fareConfigRepo;
    private final OpenRouteService openRouteService;
    private final LineRepository lineRepository;
    private static final double EARTH_RADIUS_KM = 6371;

    public FareCalculationService(FareConfigurationRepository fareConfigRepo, 
                                  OpenRouteService openRouteService,
                                  LineRepository lineRepository) {
        this.fareConfigRepo = fareConfigRepo;
        this.openRouteService = openRouteService;
        this.lineRepository = lineRepository;
    }

    /**
     * Computes distance between two stations following the path of the line.
     * This calculates the sum of distances between consecutive stations from
     * the start station to the end station in the order they appear in the line,
     * supporting both forward and reverse travel and stations on multiple lines.
     */
    public double computeDistance(Station start, Station end, Journey journey) {
        if (start.equals(end)) {
            return 0.0;
        }

        List<Station> lineStations = journey.getLine().getStations();
        
        // Find indices of start and end stations
        int startIndex = -1;
        int endIndex = -1;
        
        for (int i = 0; i < lineStations.size(); i++) {
            Station station = lineStations.get(i);
            if (station.getId().equals(start.getId())) {
                startIndex = i;
            }
            if (station.getId().equals(end.getId())) {
                endIndex = i;
            }
        }
        
        // If either station is not found on the specified line
        if (startIndex == -1 || endIndex == -1) {
            // Log the issue for debugging
            System.out.println("Warning: Station not found on line " + journey.getLine().getCode() + 
                ". Start station ID: " + start.getId() + ", End station ID: " + end.getId());
            
            // Need to fetch the line to safely check if the stations belong to it
            // getLines() might return null until the relationship is properly initialized
            try {
                Line line = lineRepository.findById(journey.getLine().getId()).orElse(null);
                
                if (line != null) {
                    boolean startOnLine = false;
                    boolean endOnLine = false;
                    
                    // Manually check if stations are on the line
                    for (Station station : line.getStations()) {
                        if (station.getId().equals(start.getId())) {
                            startOnLine = true;
                        }
                        if (station.getId().equals(end.getId())) {
                            endOnLine = true;
                        }
                    }
                    
                    if (!startOnLine || !endOnLine) {
                        System.out.println("Error: One or both stations are not on the specified line. " + 
                            "StartOnLine: " + startOnLine + ", EndOnLine: " + endOnLine);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error checking station-line relationship: " + e.getMessage());
            }
            
            // Fallback to direct distance using OpenRouteService
            return openRouteService.calculateDistance(start, end);
        }
        
        // Calculate distance between stations along the line
        double totalDistance = 0.0;
        
        // If traveling forward on the line (startIndex < endIndex)
        if (startIndex < endIndex) {
            for (int i = startIndex; i < endIndex; i++) {
                Station current = lineStations.get(i);
                Station next = lineStations.get(i + 1);
                totalDistance += openRouteService.calculateDistance(current, next);
            }
        } 
        // If traveling backward on the line (startIndex > endIndex)
        else if (startIndex > endIndex) {
            for (int i = startIndex; i > endIndex; i--) {
                Station current = lineStations.get(i);
                Station prev = lineStations.get(i - 1);
                totalDistance += openRouteService.calculateDistance(current, prev);
            }
        }
        
        return totalDistance;
    }
    
    /**
     * Backwards compatibility method
     */
    public double computeDistance(Station a, Station b) {
        return openRouteService.calculateDistance(a, b);
    }

    /**
     * Calculate fare based on a simplified flat rate per kilometer
     */
    public BigDecimal calculateFare(Journey j) {
        FareConfiguration cfg = fareConfigRepo.findActiveConfig(LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("No active fare configuration"));

        // Calculate distance if not already set
        if (j.getDistanceKm() == null) {
            j.setDistanceKm(computeDistance(j.getStartStation(), j.getEndStation(), j));
        }

        // Simple price calculation: basePricePerKm * distance
        return cfg.getBasePricePerKm()
                .multiply(BigDecimal.valueOf(j.getDistanceKm()))
                .setScale(2, RoundingMode.HALF_UP);
    }
} 