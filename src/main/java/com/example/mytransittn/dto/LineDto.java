package com.example.mytransittn.dto;

import com.example.mytransittn.model.Line;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class LineDto {
    private Long id;
    private String code;
    private BigDecimal fareMultiplier;
    private List<StationSimpleDto> stations;

    public static LineDto fromEntity(Line line) {
        LineDto dto = new LineDto();
        dto.setId(line.getId());
        dto.setCode(line.getCode());
        dto.setFareMultiplier(line.getFareMultiplier());
        
        if (line.getStations() != null) {
            dto.setStations(line.getStations().stream()
                .map(StationSimpleDto::fromEntity)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    @Data
    public static class LineSimpleDto {
        private Long id;
        private String code;
        
        public static LineSimpleDto fromEntity(com.example.mytransittn.model.Line line) {
            LineSimpleDto dto = new LineSimpleDto();
            dto.setId(line.getId());
            dto.setCode(line.getCode());
            return dto;
        }
    }
    
    // Simple representation of Station to avoid circular references
    @Data
    public static class StationSimpleDto {
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        private com.example.mytransittn.model.Station.StationStatus status;
        
        public static StationSimpleDto fromEntity(com.example.mytransittn.model.Station station) {
            StationSimpleDto dto = new StationSimpleDto();
            dto.setId(station.getId());
            dto.setName(station.getName());
            dto.setLatitude(station.getLatitude());
            dto.setLongitude(station.getLongitude());
            dto.setStatus(station.getStatus());
            return dto;
        }
    }
} 