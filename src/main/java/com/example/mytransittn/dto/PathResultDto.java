package com.example.mytransittn.dto;

import com.example.mytransittn.model.LineSegment;
import com.example.mytransittn.model.PathResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathResultDto {
    private List<StationDto> stations;
    private List<LineSegmentDto> segments;
    private double totalDistance;
    
    public static PathResultDto fromEntity(PathResult pathResult) {
        if (pathResult == null) {
            return null;
        }
        
        List<StationDto> stationDtos = pathResult.getStations().stream()
                .map(StationDto::fromEntity)
                .collect(Collectors.toList());
                
        List<LineSegmentDto> segmentDtos = pathResult.getSegments().stream()
                .map(LineSegmentDto::fromEntity)
                .collect(Collectors.toList());
                
        return new PathResultDto(stationDtos, segmentDtos, pathResult.getTotalDistance());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineSegmentDto {
        private LineDto.LineSimpleDto line;
        private StationDto from;
        private StationDto to;
        
        public static LineSegmentDto fromEntity(LineSegment segment) {
            if (segment == null) {
                return null;
            }
            
            LineDto.LineSimpleDto lineDto = new LineDto.LineSimpleDto();
            lineDto.setId(segment.getLine().getId());
            lineDto.setCode(segment.getLine().getCode());
            
            return new LineSegmentDto(
                lineDto,
                StationDto.fromEntity(segment.getFrom()),
                StationDto.fromEntity(segment.getTo())
            );
        }
    }
} 