package com.example.mytransittn.dto;

import com.example.mytransittn.model.Journey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyDto {
    private Long id;
    private StationSummaryDto startStation;
    private StationSummaryDto endStation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Journey.JourneyStatus status;
    private Double distanceKm;
    private BigDecimal fare;
    private LineSummaryDto primaryLine; // Primary line (first line in a multi-line journey)
    private UserSummaryDto user;
    private String error; // For error messages
    
    // New field for multi-line journeys
    private List<JourneySegmentDto> segments = new ArrayList<>();
    private boolean isMultiLineJourney = false;

    public static JourneyDto fromEntity(Journey journey) {
        if (journey == null) {
            return null;
        }
        
        JourneyDto dto = new JourneyDto();
        dto.setId(journey.getId());
        dto.setStartTime(journey.getStartTime());
        dto.setEndTime(journey.getEndTime());
        dto.setStatus(journey.getStatus());
        dto.setDistanceKm(journey.getDistanceKm());
        dto.setFare(journey.getFare());
        
        if (journey.getStartStation() != null) {
            StationSummaryDto startStationDto = new StationSummaryDto();
            startStationDto.setId(journey.getStartStation().getId());
            startStationDto.setName(journey.getStartStation().getName());
            dto.setStartStation(startStationDto);
        }
        
        if (journey.getEndStation() != null) {
            StationSummaryDto endStationDto = new StationSummaryDto();
            endStationDto.setId(journey.getEndStation().getId());
            endStationDto.setName(journey.getEndStation().getName());
            dto.setEndStation(endStationDto);
        }
        
        if (journey.getLine() != null) {
            LineSummaryDto lineDto = new LineSummaryDto();
            lineDto.setId(journey.getLine().getId());
            lineDto.setCode(journey.getLine().getCode());
            dto.setPrimaryLine(lineDto);
        }
        
        if (journey.getUser() != null) {
            UserSummaryDto userDto = new UserSummaryDto();
            userDto.setId(journey.getUser().getId());
            userDto.setUsername(journey.getUser().getUsername());
            userDto.setEmail(journey.getUser().getEmail());
            dto.setUser(userDto);
        }
        
        return dto;
    }
    
    @Data
    public static class StationSummaryDto {
        private Long id;
        private String name;
    }
    
    @Data
    public static class LineSummaryDto {
        private Long id;
        private String code;
    }
    
    @Data
    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String email;
    }
    
    @Data
    public static class JourneyRequestDto {
        private Long startStationId;
        private Long endStationId;
        private Long lineId; // Optional: if not provided, system will find the best route
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JourneySegmentDto {
        private LineSummaryDto line;
        private StationSummaryDto startStation;
        private StationSummaryDto endStation;
        private Double distanceKm;
        private boolean isTransfer;
    }
} 