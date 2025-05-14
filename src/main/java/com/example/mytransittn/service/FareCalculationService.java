package com.example.mytransittn.service;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.FareConfigurationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FareCalculationService {
    private final FareConfigurationRepository fareConfigRepo;
    private static final double EARTH_RADIUS_KM = 6371;

    public FareCalculationService(FareConfigurationRepository fareConfigRepo) {
        this.fareConfigRepo = fareConfigRepo;
    }

    /**
     * Computes distance between two stations following the path of the line.
     * This calculates the sum of distances between consecutive stations from
     * the start station to the end station in the order they appear in the line.
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
        
        // If stations not found in the line
        if (startIndex == -1 || endIndex == -1) {
            // Fallback to direct distance
            return calculateDirectDistance(start, end);
        }
        
        // Ensure proper direction (handle if traveling backward on the line)
        if (startIndex > endIndex) {
            int temp = startIndex;
            startIndex = endIndex;
            endIndex = temp;
        }
        
        // Calculate distance between consecutive stations
        double totalDistance = 0.0;
        for (int i = startIndex; i < endIndex; i++) {
            Station current = lineStations.get(i);
            Station next = lineStations.get(i + 1);
            totalDistance += calculateDirectDistance(current, next);
        }
        
        return totalDistance;
    }
    
    /**
     * Calculate direct distance between two stations using Haversine formula
     */
    private double calculateDirectDistance(Station a, Station b) {
        double lat1 = Math.toRadians(a.getLatitude()),
               lon1 = Math.toRadians(a.getLongitude()),
               lat2 = Math.toRadians(b.getLatitude()),
               lon2 = Math.toRadians(b.getLongitude());
        double dLat = lat2 - lat1, dLon = lon2 - lon1;
        double h = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(lat1) * Math.cos(lat2)
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1-h));
    }
    
    /**
     * Backwards compatibility method
     */
    public double computeDistance(Station a, Station b) {
        return calculateDirectDistance(a, b);
    }

    public BigDecimal calculateFare(Journey j) {
        FareConfiguration cfg = fareConfigRepo.findActiveConfig(LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("No active fare configuration"));

        // Calculate distance if not already set
        if (j.getDistanceKm() == null) {
            j.setDistanceKm(computeDistance(j.getStartStation(), j.getEndStation(), j));
        }

        // 1. Base distance fare
        BigDecimal distFare = cfg.getBasePricePerKm()
                .multiply(BigDecimal.valueOf(j.getDistanceKm()));

        // 2. Line multiplier
        BigDecimal lineMul = j.getLine().getFareMultiplier();

        // 3. State multiplier (average start/end)
        BigDecimal s1 = j.getStartStation().getState().getPriceMultiplier();
        BigDecimal s2 = j.getEndStation().getState().getPriceMultiplier();
        BigDecimal stateMul = s1.add(s2).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);

        BigDecimal base = distFare.multiply(lineMul).multiply(stateMul);

        // 4. Peak/off-peak
        boolean isPeak = isPeakHour(j.getStartTime());
        BigDecimal timeMul = isPeak
                ? cfg.getPeakHourMultiplier()
                : cfg.getOffPeakHourMultiplier();
        BigDecimal fare = base.multiply(timeMul);

        // 5. Apply min/max caps
        return fare.max(cfg.getMinimumFare())
                .min(cfg.getMaximumFare())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= 7 && hour < 9) || (hour >= 17 && hour < 19);
    }
} 