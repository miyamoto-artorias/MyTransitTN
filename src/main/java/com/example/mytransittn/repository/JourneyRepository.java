package com.example.mytransittn.repository;

import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JourneyRepository extends JpaRepository<Journey, Long> {
    List<Journey> findByUser(User user);
    
    List<Journey> findByUserOrderByStartTimeDesc(User user);
    
    @Query("SELECT j FROM Journey j WHERE j.user = :user AND j.status = :status")
    List<Journey> findByUserAndStatus(@Param("user") User user, @Param("status") Journey.JourneyStatus status);
    
    @Query("SELECT j FROM Journey j WHERE j.startTime BETWEEN :start AND :end")
    List<Journey> findJourneysBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT j FROM Journey j WHERE j.startStation.id = :stationId OR j.endStation.id = :stationId")
    List<Journey> findJourneysByStation(@Param("stationId") Long stationId);
} 