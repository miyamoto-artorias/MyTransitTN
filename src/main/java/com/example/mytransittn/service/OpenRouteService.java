package com.example.mytransittn.service;

import com.example.mytransittn.model.Station;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenRouteService {

    private static final String DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    
    // Cache for distance calculations to avoid redundant API calls
    private final Map<String, Double> distanceCache = new HashMap<>();

    public OpenRouteService(RestTemplate restTemplate, @Value("${openroute.api.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Calculate distance between two stations
     * @param start Start station
     * @param end End station
     * @return distance in kilometers
     */
    public double calculateDistance(Station start, Station end) {
        // Create a cache key
        String cacheKey = start.getId() + "-" + end.getId();
        
        // Check if already in cache
        if (distanceCache.containsKey(cacheKey)) {
            return distanceCache.get(cacheKey);
        }
        
        try {
            // For now, always use the direct distance calculation to avoid API issues
            double distance = calculateDirectDistance(start, end);
            
            // Store in cache for future use
            distanceCache.put(cacheKey, distance);
            return distance;
            
            /*
            // NOTE: API integration commented out until API key issues are resolved
            // Would implement proper API request here if needed in the future
            */
        } catch (Exception e) {
            // Log error and fall back to direct calculation
            System.err.println("Error calculating distance: " + e.getMessage());
            return calculateDirectDistance(start, end);
        }
    }
    
    /**
     * Calculate direct distance between two stations using Haversine formula
     */
    private double calculateDirectDistance(Station a, Station b) {
        try {
            final double EARTH_RADIUS_KM = 6371;
            
            // Check for null coordinates and provide fallback
            if (a.getLatitude() == null || a.getLongitude() == null || 
                b.getLatitude() == null || b.getLongitude() == null) {
                System.out.println("Warning: Station coordinates missing, using default distance of 5km");
                return 5.0; // Default fallback distance
            }
            
            double lat1 = Math.toRadians(a.getLatitude()),
                   lon1 = Math.toRadians(a.getLongitude()),
                   lat2 = Math.toRadians(b.getLatitude()),
                   lon2 = Math.toRadians(b.getLongitude());
            double dLat = lat2 - lat1, dLon = lon2 - lon1;
            double h = Math.sin(dLat/2) * Math.sin(dLat/2)
                     + Math.cos(lat1) * Math.cos(lat2)
                     * Math.sin(dLon/2) * Math.sin(dLon/2);
            return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1-h));
        } catch (Exception e) {
            System.err.println("Error in Haversine calculation: " + e.getMessage());
            return 5.0; // Default fallback if calculation fails
        }
    }
    
    /**
     * Clear the distance cache
     */
    public void clearCache() {
        distanceCache.clear();
    }
} 