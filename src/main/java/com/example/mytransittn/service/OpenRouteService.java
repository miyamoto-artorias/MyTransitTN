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

    public OpenRouteService(RestTemplate restTemplate, @Value("${openroute.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Calculate distance between two stations using OpenRouteService
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
            // Validate coordinates - some databases might have null values
            if (start.getLatitude() == null || start.getLongitude() == null || 
                end.getLatitude() == null || end.getLongitude() == null) {
                return calculateDirectDistance(start, end);
            }
            
            HttpHeaders headers = new HttpHeaders();
            // OpenRouteService API expects the API key in an 'Authorization' header
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create the request body
            Map<String, Object> requestBody = new HashMap<>();
            
            // Format coordinates as [longitude, latitude] for OpenRouteService
            List<List<Double>> coordinates = new ArrayList<>();
            coordinates.add(Arrays.asList(start.getLongitude(), start.getLatitude()));
            coordinates.add(Arrays.asList(end.getLongitude(), end.getLatitude()));
            
            requestBody.put("coordinates", coordinates);
            
            // Create the HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Make the POST request
            ResponseEntity<String> response = restTemplate.exchange(
                DIRECTIONS_URL,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // Log response for debugging
            System.out.println("OpenRouteService API response status: " + response.getStatusCode());
            
            // Parse the response to get the distance in kilometers
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode routes = root.path("routes");
                
                if (routes.isArray() && routes.size() > 0) {
                    JsonNode summary = routes.get(0).path("summary");
                    double distanceMeters = summary.path("distance").asDouble();
                    double distanceKm = distanceMeters / 1000.0;
                    
                    // Store in cache for future use
                    distanceCache.put(cacheKey, distanceKm);
                    return distanceKm;
                }
            }
            
            // If we couldn't get the distance from the API, fall back to direct calculation
            return calculateDirectDistance(start, end);
        } catch (Exception e) {
            // Log error and fall back to direct calculation
            System.err.println("Error calling OpenRouteService: " + e.getMessage());
            return calculateDirectDistance(start, end);
        }
    }
    
    /**
     * Calculate direct distance between two stations using Haversine formula
     * This is a fallback method if the API call fails
     */
    private double calculateDirectDistance(Station a, Station b) {
        final double EARTH_RADIUS_KM = 6371;
        
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
     * Clear the distance cache
     */
    public void clearCache() {
        distanceCache.clear();
    }
} 