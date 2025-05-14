package com.example.mytransittn.model;

/**
 * Represents a segment of a journey on a specific line.
 * Used for path finding results to show which line to take and between which stations.
 */
public class LineSegment {
    private Line line;
    private Station from;
    private Station to;

    public LineSegment() {}

    public LineSegment(Line line, Station from, Station to) {
        this.line = line;
        this.from = from;
        this.to = to;
    }

    public Line getLine() {
        return line;
    }

    public void setLine(Line line) {
        this.line = line;
    }

    public Station getFrom() {
        return from;
    }

    public void setFrom(Station from) {
        this.from = from;
    }

    public Station getTo() {
        return to;
    }

    public void setTo(Station to) {
        this.to = to;
    }
} 