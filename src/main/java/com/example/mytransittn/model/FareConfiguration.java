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

    /** base price per km - the only fare parameter we now use */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal basePricePerKm;

    @Column(nullable = false)
    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    private ConfigStatus status;

    public enum ConfigStatus { ACTIVE, INACTIVE, SCHEDULED }
    
    // Explicit getter to ensure it's available even if Lombok doesn't generate it
    public BigDecimal getBasePricePerKm() {
        return basePricePerKm;
    }

    public void setBasePricePerKm(BigDecimal basePricePerKm) {
        this.basePricePerKm = basePricePerKm;
    }
} 