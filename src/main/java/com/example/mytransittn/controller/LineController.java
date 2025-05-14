package com.example.mytransittn.controller;

import com.example.mytransittn.dto.LineDto;
import com.example.mytransittn.dto.StationDto;
import com.example.mytransittn.model.Line;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.LineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lines")
public class LineController {

    private final LineRepository lineRepository;

    @Autowired
    public LineController(LineRepository lineRepository) {
        this.lineRepository = lineRepository;
    }

    @GetMapping
    public ResponseEntity<List<LineDto>> getAllLines() {
        List<LineDto> lineDtos = lineRepository.findAll().stream()
                .map(LineDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lineDtos);
    }

    @GetMapping("/with-stations")
    public ResponseEntity<List<LineDto>> getAllLinesWithStations() {
        List<LineDto> lineDtos = lineRepository.findAllWithStations().stream()
                .map(LineDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lineDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LineDto> getLineById(@PathVariable Long id) {
        return lineRepository.findById(id)
                .map(LineDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<LineDto> getLineByCode(@PathVariable String code) {
        return lineRepository.findByCode(code)
                .map(LineDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stations")
    public ResponseEntity<List<StationDto>> getLineStations(@PathVariable Long id) {
        return lineRepository.findById(id)
                .map(line -> line.getStations().stream()
                        .map(StationDto::fromEntity)
                        .collect(Collectors.toList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
} 