package com.example.mytransittn.controller;

import com.example.mytransittn.model.Line;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.repository.LineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lines")
public class LineController {

    private final LineRepository lineRepository;

    @Autowired
    public LineController(LineRepository lineRepository) {
        this.lineRepository = lineRepository;
    }

    @GetMapping
    public ResponseEntity<List<Line>> getAllLines() {
        return ResponseEntity.ok(lineRepository.findAll());
    }

    @GetMapping("/with-stations")
    public ResponseEntity<List<Line>> getAllLinesWithStations() {
        return ResponseEntity.ok(lineRepository.findAllWithStations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Line> getLineById(@PathVariable Long id) {
        return lineRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Line> getLineByCode(@PathVariable String code) {
        return lineRepository.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stations")
    public ResponseEntity<List<Station>> getLineStations(@PathVariable Long id) {
        return lineRepository.findById(id)
                .map(line -> ResponseEntity.ok(line.getStations()))
                .orElse(ResponseEntity.notFound().build());
    }
} 