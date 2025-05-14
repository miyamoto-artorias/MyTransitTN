package com.example.mytransittn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a path finding operation.
 * Contains the full sequence of stations to visit, the line segments to use,
 * and the total distance of the journey.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathResult {
    private List<Station> stations;
    private List<LineSegment> segments;
    private double totalDistance;
} 