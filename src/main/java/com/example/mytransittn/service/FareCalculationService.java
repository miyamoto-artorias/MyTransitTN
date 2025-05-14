package com.example.mytransittn.service;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.FareConfigurationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class FareCalculationService {
    private final FareConfigurationRepository fareConfigRepo;
    private static final double EARTH_RADIUS_KM = 6371;

    public FareCalculationService(FareConfigurationRepository fareConfigRepo) {
        this.fareConfigRepo = fareConfigRepo;
    }

    public double computeDistance(Station a, Station b) {
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

    public BigDecimal calculateFare(Journey j) {
        FareConfiguration cfg = fareConfigRepo.findActiveConfig(LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("No active fare configuration"));

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