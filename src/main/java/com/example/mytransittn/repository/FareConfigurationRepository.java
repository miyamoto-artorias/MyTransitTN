package com.example.mytransittn.repository;

import com.example.mytransittn.model.FareConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FareConfigurationRepository extends JpaRepository<FareConfiguration, Long> {
    
    @Query("SELECT fc FROM FareConfiguration fc WHERE " +
           "fc.status = com.example.mytransittn.model.FareConfiguration$ConfigStatus.ACTIVE " +
           "AND fc.effectiveFrom <= :time " +
           "AND (fc.effectiveTo IS NULL OR fc.effectiveTo > :time) " +
           "ORDER BY fc.effectiveFrom DESC")
    Optional<FareConfiguration> findActiveConfig(@Param("time") LocalDateTime time);
} 