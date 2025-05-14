package com.example.mytransittn.model;

import java.util.List;

/**
 * Result of a path finding operation.
 * Contains the full sequence of stations to visit, the line segments to use,
 * and the total distance of the journey.
 */

public class PathResult {
    private List<Station> stations;
    private List<LineSegment> segments;
    private double totalDistance;

    public PathResult() {}

    public PathResult(List<Station> stations, List<LineSegment> segments, double totalDistance) {
        this.stations = stations;
        this.segments = segments;
        this.totalDistance = totalDistance;
    }

    public List<Station> getStations() {
        return stations;
    }

    public void setStations(List<Station> stations) {
        this.stations = stations;
    }

    public List<LineSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<LineSegment> segments) {
        this.segments = segments;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
} 