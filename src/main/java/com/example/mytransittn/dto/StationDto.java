package com.example.mytransittn.dto;

import com.example.mytransittn.model.Station;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class StationDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Long stateId;
    private String stateName;
    private Station.StationStatus status;
    private List<LineSimpleDto> lines;

    public static StationDto fromEntity(Station station) {
        StationDto dto = new StationDto();
        dto.setId(station.getId());
        dto.setName(station.getName());
        dto.setLatitude(station.getLatitude());
        dto.setLongitude(station.getLongitude());
        dto.setStatus(station.getStatus());
        
        if (station.getState() != null) {
            dto.setStateId(station.getState().getId());
            dto.setStateName(station.getState().getName());
        }
        
        if (station.getLines() != null) {
            dto.setLines(station.getLines().stream()
                .map(LineSimpleDto::fromEntity)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    // Simple representation of Line to avoid circular references
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
} 