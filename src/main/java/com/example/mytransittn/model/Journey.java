package com.example.mytransittn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.mytransittn.service.FareCalculationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "journeys")
@EntityListeners(Journey.JourneyListener.class)
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



    public static class JourneyListener {
        @Autowired
        private FareCalculationService fareService;

        @PrePersist @PreUpdate
        public void onSave(Journey j) {
            if (j.getStatus() == JourneyStatus.COMPLETED && j.getEndTime() != null) {
                j.setDistanceKm(fareService.computeDistance(
                        j.getStartStation(), j.getEndStation()));
                j.setFare(fareService.calculateFare(j));
            }
        }
    }
} 