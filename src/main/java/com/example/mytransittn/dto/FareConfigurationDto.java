package com.example.mytransittn.dto;

import com.example.mytransittn.model.FareConfiguration;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FareConfigurationDto {
    private Long id;
    private BigDecimal basePricePerKm;
    private BigDecimal minimumFare;
    private BigDecimal maximumFare;
    private BigDecimal peakHourMultiplier;
    private BigDecimal offPeakHourMultiplier;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private FareConfiguration.ConfigStatus status;

    public static FareConfigurationDto fromEntity(FareConfiguration fareConfiguration) {
        if (fareConfiguration == null) {
            return null;
        }
        
        FareConfigurationDto dto = new FareConfigurationDto();
        dto.setId(fareConfiguration.getId());
        dto.setBasePricePerKm(fareConfiguration.getBasePricePerKm());
        dto.setMinimumFare(fareConfiguration.getMinimumFare());
        dto.setMaximumFare(fareConfiguration.getMaximumFare());
        dto.setPeakHourMultiplier(fareConfiguration.getPeakHourMultiplier());
        dto.setOffPeakHourMultiplier(fareConfiguration.getOffPeakHourMultiplier());
        dto.setEffectiveFrom(fareConfiguration.getEffectiveFrom());
        dto.setEffectiveTo(fareConfiguration.getEffectiveTo());
        dto.setStatus(fareConfiguration.getStatus());
        
        return dto;
    }
} 