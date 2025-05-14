package com.example.mytransittn.service;

import com.example.mytransittn.model.*;
import com.example.mytransittn.repository.LineRepository;
import com.example.mytransittn.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NavigationService {
    private final StationRepository stationRepository;
    private final LineRepository lineRepository;
    private final FareCalculationService fareCalculationService;

    public NavigationService(StationRepository stationRepository, LineRepository lineRepository, FareCalculationService fareCalculationService) {
        this.stationRepository = stationRepository;
        this.lineRepository = lineRepository;
        this.fareCalculationService = fareCalculationService;
    }

    /**
     * Finds the shortest path (by distance) from start to end,
     * returning the ordered list of stations and the lines you'll take.
     */
    public PathResult findShortestPath(Long startId, Long endId) {
        Station start = stationRepository.findById(startId).orElseThrow(() -> 
                new IllegalArgumentException("Start station not found"));
        Station end = stationRepository.findById(endId).orElseThrow(() -> 
                new IllegalArgumentException("End station not found"));

        // Build the graph: stations connected by edges with weights = distance
        Map<Station, List<Edge>> graph = buildGraph();

        // Run Dijkstra's algorithm
        Map<Station, Double> dist = new HashMap<>();
        Map<Station, Station> prev = new HashMap<>();
        PriorityQueue<Station> pq = new PriorityQueue<>(Comparator.comparing(dist::get));
        
        // Initialize
        for (Station station : graph.keySet()) {
            dist.put(station, Double.MAX_VALUE);
        }
        dist.put(start, 0.0);
        pq.add(start);

        // Find shortest path
        while (!pq.isEmpty()) {
            Station current = pq.poll();
            if (current.equals(end)) break;
            
            for (Edge edge : graph.getOrDefault(current, Collections.emptyList())) {
                double alt = dist.get(current) + edge.getWeight();
                if (alt < dist.getOrDefault(edge.getTo(), Double.MAX_VALUE)) {
                    dist.put(edge.getTo(), alt);
                    prev.put(edge.getTo(), current);
                    
                    // Update priority queue
                    pq.remove(edge.getTo());
                    pq.add(edge.getTo());
                }
            }
        }

        // Reconstruct path
        List<Station> stationPath = reconstructPath(prev, start, end);
        if (stationPath.isEmpty()) {
            return new PathResult(Collections.emptyList(), Collections.emptyList(), 0);
        }

        // Infer line segments
        List<LineSegment> segments = inferLineSegments(stationPath);

        return new PathResult(stationPath, segments, dist.getOrDefault(end, 0.0));
    }

    private Map<Station, List<Edge>> buildGraph() {
        Map<Station, List<Edge>> graph = new HashMap<>();
        
        // For each line
        List<Line> allLines = getAllLines();
        for (Line line : allLines) {
            List<Station> stations = line.getStations();
            
            // Connect adjacent stations
            for (int i = 0; i < stations.size() - 1; i++) {
                Station from = stations.get(i);
                Station to = stations.get(i + 1);
                double distance = fareCalculationService.computeDistance(from, to);
                
                // Add edges in both directions (graph is undirected)
                addEdge(graph, from, to, distance, line);
                addEdge(graph, to, from, distance, line);
            }
        }
        
        return graph;
    }

    private void addEdge(Map<Station, List<Edge>> graph, Station from, Station to, double weight, Line line) {
        graph.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(to, weight, line));
    }

    private List<Station> reconstructPath(Map<Station, Station> prev, Station start, Station end) {
        List<Station> path = new ArrayList<>();
        
        // If end is not reachable
        if (!prev.containsKey(end) && !end.equals(start)) {
            return path;
        }
        
        // Reconstruct from end to start
        Station current = end;
        while (current != null) {
            path.add(current);
            current = prev.get(current);
        }
        
        // Reverse to get path from start to end
        Collections.reverse(path);
        return path;
    }

    private List<LineSegment> inferLineSegments(List<Station> stationPath) {
        List<LineSegment> segments = new ArrayList<>();
        if (stationPath.size() < 2) return segments;

        Station current = stationPath.get(0);
        Line currentLine = null;
        Station segmentStart = current;

        for (int i = 1; i < stationPath.size(); i++) {
            Station next = stationPath.get(i);
            
            // Find a line that connects current and next
            Line connectingLine = findConnectingLine(current, next);
            
            // If no line found or line changed, end current segment and start new one
            if (connectingLine == null) {
                throw new IllegalStateException("No line connects " + current.getName() + " and " + next.getName());
            }
            
            if (currentLine == null) {
                currentLine = connectingLine;
            } else if (!currentLine.equals(connectingLine)) {
                // Line changed, add segment and start new one
                segments.add(new LineSegment(currentLine, segmentStart, current));
                segmentStart = current;
                currentLine = connectingLine;
            }
            
            current = next;
        }
        
        // Add the last segment
        if (currentLine != null) {
            segments.add(new LineSegment(currentLine, segmentStart, current));
        }
        
        return segments;
    }

    private Line findConnectingLine(Station s1, Station s2) {
        Set<Line> s1Lines = s1.getLines();
        Set<Line> s2Lines = s2.getLines();
        
        // Find lines common to both stations
        Set<Line> commonLines = s1Lines.stream()
                .filter(s2Lines::contains)
                .collect(Collectors.toSet());
        
        // Return first common line, if any
        return commonLines.isEmpty() ? null : commonLines.iterator().next();
    }
    
    // Helper method to get all lines
    private List<Line> getAllLines() {
        return lineRepository.findAllWithStations();
    }
    
    // Edge class for graph representation
    private static class Edge {
        private Station to;
        private double weight;
        private Line line;
        
        public Edge(Station to, double weight, Line line) {
            this.to = to;
            this.weight = weight;
            this.line = line;
        }
        
        public Station getTo() {
            return to;
        }
        
        public double getWeight() {
            return weight;
        }
        
        public Line getLine() {
            return line;
        }
    }
} 