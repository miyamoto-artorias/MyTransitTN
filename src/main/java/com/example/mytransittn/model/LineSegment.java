package com.example.mytransittn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a segment of a journey on a specific line.
 * Used for path finding results to show which line to take and between which stations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineSegment {
    private Line line;
    private Station from;
    private Station to;
} 