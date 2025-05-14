package com.example.mytransittn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "fare_configuration")
public class FareConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** base price per km */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal basePricePerKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumFare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maximumFare;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal peakHourMultiplier;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal offPeakHourMultiplier;

    @Column(nullable = false)
    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    private ConfigStatus status;

    public enum ConfigStatus { ACTIVE, INACTIVE, SCHEDULED }
    
    // Explicit getters to ensure they're available even if Lombok doesn't generate them
    public BigDecimal getBasePricePerKm() {
        return basePricePerKm;
    }
    
    public BigDecimal getMinimumFare() {
        return minimumFare;
    }
    
    public BigDecimal getMaximumFare() {
        return maximumFare;
    }
    
    public BigDecimal getPeakHourMultiplier() {
        return peakHourMultiplier;
    }
    
    public BigDecimal getOffPeakHourMultiplier() {
        return offPeakHourMultiplier;
    }

    public void setBasePricePerKm(BigDecimal basePricePerKm) {
        this.basePricePerKm = basePricePerKm;
    }

} 