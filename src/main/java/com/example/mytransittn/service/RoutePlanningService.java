package com.example.mytransittn.service;

import com.example.mytransittn.model.Line;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.LineRepository;
import com.example.mytransittn.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service to handle route planning across multiple transit lines
 */
@Service
public class RoutePlanningService {

    private final LineRepository lineRepository;
    private final StationRepository stationRepository;
    private final OpenRouteService openRouteService;

    @Autowired
    public RoutePlanningService(LineRepository lineRepository, 
                                StationRepository stationRepository,
                                OpenRouteService openRouteService) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
        this.openRouteService = openRouteService;
    }

    /**
     * Data structure to represent a journey between stations that might involve line changes
     */
    public static class JourneyPlan {
        private final List<JourneySegment> segments = new ArrayList<>();
        private double totalDistance = 0.0;
        
        public List<JourneySegment> getSegments() {
            return Collections.unmodifiableList(segments);
        }
        
        public void addSegment(JourneySegment segment) {
            segments.add(segment);
            totalDistance += segment.getDistance();
        }
        
        public double getTotalDistance() {
            return totalDistance;
        }
        
        public boolean isEmpty() {
            return segments.isEmpty();
        }
        
        public Line getPrimaryLine() {
            // For simplicity, return the line of the first segment as the "primary" line
            if (!segments.isEmpty()) {
                return segments.get(0).getLine();
            }
            return null;
        }
    }
    
    /**
     * Represents one segment of a journey on a single line
     */
    public static class JourneySegment {
        private final Line line;
        private final Station startStation;
        private final Station endStation;
        private final double distance;
        private final boolean isTransfer;
        
        public JourneySegment(Line line, Station startStation, Station endStation, double distance, boolean isTransfer) {
            this.line = line;
            this.startStation = startStation;
            this.endStation = endStation;
            this.distance = distance;
            this.isTransfer = isTransfer;
        }
        
        public Line getLine() {
            return line;
        }
        
        public Station getStartStation() {
            return startStation;
        }
        
        public Station getEndStation() {
            return endStation;
        }
        
        public double getDistance() {
            return distance;
        }
        
        public boolean isTransfer() {
            return isTransfer;
        }
    }

    /**
     * Find the most efficient route between two stations, potentially across multiple lines
     * @param startStationId Start station ID
     * @param endStationId End station ID
     * @return A JourneyPlan with one or more segments, or null if no route is found
     */
    public JourneyPlan findRoute(Long startStationId, Long endStationId) {
        Station startStation = stationRepository.findById(startStationId)
            .orElseThrow(() -> new IllegalArgumentException("Start station not found"));
        
        Station endStation = stationRepository.findById(endStationId)
            .orElseThrow(() -> new IllegalArgumentException("End station not found"));
        
        // Check if stations are the same
        if (startStationId.equals(endStationId)) {
            throw new IllegalArgumentException("Start and end stations cannot be the same");
        }
        
        // First, try to find a direct route on a single line
        JourneyPlan directRoute = findDirectRoute(startStation, endStation);
        if (directRoute != null) {
            return directRoute;
        }
        
        // If no direct route, try to find a route with transfers
        return findRouteWithTransfers(startStation, endStation);
    }
    
    /**
     * Check if two stations are on the same line and create a direct route if possible
     */
    private JourneyPlan findDirectRoute(Station startStation, Station endStation) {
        // Get all lines that contain the start station
        Set<Line> startStationLines = startStation.getLines();
        
        // For each line containing the start station, check if it also contains the end station
        for (Line line : startStationLines) {
            List<Station> lineStations = line.getStations();
            
            int startIndex = -1;
            int endIndex = -1;
            
            // Find positions of both stations on this line
            for (int i = 0; i < lineStations.size(); i++) {
                Station station = lineStations.get(i);
                if (station.getId().equals(startStation.getId())) {
                    startIndex = i;
                }
                if (station.getId().equals(endStation.getId())) {
                    endIndex = i;
                }
            }
            
            // If both stations are on this line, we can create a direct route
            if (startIndex != -1 && endIndex != -1) {
                double distance = calculateDistanceAlongLine(line, startIndex, endIndex);
                
                JourneyPlan plan = new JourneyPlan();
                plan.addSegment(new JourneySegment(line, startStation, endStation, distance, false));
                return plan;
            }
        }
        
        // No direct route found
        return null;
    }
    
    /**
     * Find a route that requires changing lines
     */
    private JourneyPlan findRouteWithTransfers(Station startStation, Station endStation) {
        // Use Breadth-First Search to find the shortest path
        Queue<PathNode> queue = new LinkedList<>();
        Set<Long> visitedStations = new HashSet<>();
        
        // Start by adding all possible starting points
        for (Line line : startStation.getLines()) {
            queue.add(new PathNode(startStation, line, null, 0.0));
        }
        
        visitedStations.add(startStation.getId());
        
        while (!queue.isEmpty()) {
            PathNode currentNode = queue.poll();
            Station currentStation = currentNode.station;
            Line currentLine = currentNode.line;
            
            // Check if we've reached the destination
            if (currentStation.getId().equals(endStation.getId())) {
                // Reconstruct the path
                return reconstructPath(currentNode, startStation, endStation);
            }
            
            // Continue on the current line
            List<Station> nextStationsOnLine = findAdjacentStationsOnLine(currentLine, currentStation);
            for (Station nextStation : nextStationsOnLine) {
                if (!visitedStations.contains(nextStation.getId())) {
                    double distance = openRouteService.calculateDistance(currentStation, nextStation);
                    queue.add(new PathNode(nextStation, currentLine, currentNode, distance));
                    visitedStations.add(nextStation.getId());
                }
            }
            
            // Try changing to a different line at this station
            if (currentNode.parent != null) { // Don't try to change lines at the start station
                for (Line nextLine : currentStation.getLines()) {
                    if (!nextLine.equals(currentLine)) { // Different line
                        queue.add(new PathNode(currentStation, nextLine, currentNode, 0.0)); // 0 distance for transfer
                    }
                }
            }
        }
        
        // No route found
        return null;
    }
    
    /**
     * Find stations adjacent to the current station on the specified line
     */
    private List<Station> findAdjacentStationsOnLine(Line line, Station station) {
        List<Station> lineStations = line.getStations();
        List<Station> adjacentStations = new ArrayList<>();
        
        int stationIndex = -1;
        for (int i = 0; i < lineStations.size(); i++) {
            if (lineStations.get(i).getId().equals(station.getId())) {
                stationIndex = i;
                break;
            }
        }
        
        if (stationIndex == -1) {
            return adjacentStations;
        }
        
        // Add previous station if not at the start
        if (stationIndex > 0) {
            adjacentStations.add(lineStations.get(stationIndex - 1));
        }
        
        // Add next station if not at the end
        if (stationIndex < lineStations.size() - 1) {
            adjacentStations.add(lineStations.get(stationIndex + 1));
        }
        
        return adjacentStations;
    }
    
    /**
     * Reconstruct the path from the end node back to the start
     */
    private JourneyPlan reconstructPath(PathNode endNode, Station startStation, Station endStation) {
        JourneyPlan plan = new JourneyPlan();
        List<PathNode> reversePath = new ArrayList<>();
        
        // Build the reverse path
        PathNode current = endNode;
        while (current != null) {
            reversePath.add(current);
            current = current.parent;
        }
        
        // Create segments in the correct order
        for (int i = reversePath.size() - 1; i > 0; i--) {
            PathNode currentNode = reversePath.get(i);
            PathNode nextNode = reversePath.get(i - 1);
            
            // Check if this is a line change
            boolean isTransfer = !currentNode.line.equals(nextNode.line);
            
            // If it's a transfer, we don't create a movement segment
            if (!isTransfer) {
                plan.addSegment(new JourneySegment(
                    currentNode.line,
                    currentNode.station,
                    nextNode.station,
                    nextNode.distanceFromPrevious,
                    false
                ));
            } else {
                // For a transfer, create a zero-distance transfer segment
                plan.addSegment(new JourneySegment(
                    nextNode.line, // The new line
                    currentNode.station, // The transfer station
                    currentNode.station, // Same station
                    0.0,
                    true
                ));
            }
        }
        
        return plan;
    }
    
    /**
     * Calculate the distance between two stations along a line
     */
    private double calculateDistanceAlongLine(Line line, int startIndex, int endIndex) {
        List<Station> stations = line.getStations();
        double totalDistance = 0.0;
        
        // Choose the direction based on indices
        if (startIndex < endIndex) {
            // Forward direction
            for (int i = startIndex; i < endIndex; i++) {
                totalDistance += openRouteService.calculateDistance(stations.get(i), stations.get(i + 1));
            }
        } else {
            // Backward direction
            for (int i = startIndex; i > endIndex; i--) {
                totalDistance += openRouteService.calculateDistance(stations.get(i), stations.get(i - 1));
            }
        }
        
        return totalDistance;
    }
    
    /**
     * Helper class for BFS path finding
     */
    private static class PathNode {
        Station station;
        Line line;
        PathNode parent;
        double distanceFromPrevious;
        
        PathNode(Station station, Line line, PathNode parent, double distanceFromPrevious) {
            this.station = station;
            this.line = line;
            this.parent = parent;
            this.distanceFromPrevious = distanceFromPrevious;
        }
    }
} 