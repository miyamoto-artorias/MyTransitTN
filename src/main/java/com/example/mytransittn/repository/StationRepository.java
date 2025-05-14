package com.example.mytransittn.repository;

import com.example.mytransittn.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    
    Optional<Station> findByName(String name);
    
    List<Station> findByStateId(Long stateId);
    
    @Query("SELECT s FROM Station s WHERE s.status = 'OPEN'")
    List<Station> findAllOpenStations();
    
    @Query("SELECT s FROM Station s JOIN s.lines l WHERE l.id = :lineId ORDER BY l.stations.station_order")
    List<Station> findStationsByLineIdOrdered(@Param("lineId") Long lineId);
} 