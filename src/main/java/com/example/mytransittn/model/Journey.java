package com.example.mytransittn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "journeys")
public class Journey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Station startStation;

    @ManyToOne(optional = false)
    private Station endStation;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyStatus status;

    /** computed */
    private Double distanceKm;

    /** computed */
    private BigDecimal fare;

    @ManyToOne(optional = false)
    private Line line;

    @ManyToOne(optional = false)
    private User user;

    public enum JourneyStatus { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }
} 