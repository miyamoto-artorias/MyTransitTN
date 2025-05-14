package com.example.mytransittn.repository;

import com.example.mytransittn.model.Line;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LineRepository extends JpaRepository<Line, Long> {
    Optional<Line> findByCode(String code);
    
    @Query("SELECT l FROM Line l JOIN FETCH l.stations ORDER BY l.code")
    List<Line> findAllWithStations();
} 