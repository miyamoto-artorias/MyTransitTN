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
    
    @Query(value = "SELECT s.* FROM stations s " +
           "JOIN line_stations ls ON s.id = ls.station_id " +
           "WHERE ls.line_id = :lineId " +
           "ORDER BY ls.station_order", nativeQuery = true)
    List<Station> findStationsByLineIdOrdered(@Param("lineId") Long lineId);
} 