package com.example.mytransittn.controller;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.FareConfigurationRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/fares")
public class FareAdminController {

    private final FareConfigurationRepository fareConfigurationRepository;
    private final UserRepository userRepository;
    private final boolean isDevMode = true; // Set to false in production

    @Autowired
    public FareAdminController(FareConfigurationRepository fareConfigurationRepository, UserRepository userRepository) {
        this.fareConfigurationRepository = fareConfigurationRepository;
        this.userRepository = userRepository;
    }
    
    @PostMapping
    @Transactional
    public ResponseEntity<?> createFareConfiguration(@RequestBody FareConfigRequest request) {
        // Check admin authorization (bypassed in dev mode)
        if (!isDevMode) {
            User user = getCurrentUser();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        // Validate request
        if (request.getBasePricePerKm() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Base price per km is required"));
        }
        
        // Create fare configuration
        FareConfiguration fareConfig = new FareConfiguration();
        fareConfig.setBasePricePerKm(request.getBasePricePerKm());
        fareConfig.setEffectiveFrom(request.getEffectiveFrom() != null ? 
                request.getEffectiveFrom() : LocalDateTime.now());
        fareConfig.setEffectiveTo(request.getEffectiveTo());
        fareConfig.setStatus(request.getStatus() != null ? 
                request.getStatus() : FareConfiguration.ConfigStatus.ACTIVE);
        
        // If this is an active configuration, deactivate all other active configs
        if (fareConfig.getStatus() == FareConfiguration.ConfigStatus.ACTIVE) {
            fareConfigurationRepository.findActiveConfig(LocalDateTime.now())
                    .ifPresent(activeConfig -> {
                        activeConfig.setStatus(FareConfiguration.ConfigStatus.INACTIVE);
                        fareConfigurationRepository.save(activeConfig);
                    });
        }
        
        FareConfiguration savedConfig = fareConfigurationRepository.save(fareConfig);
        return ResponseEntity.ok(savedConfig);
    }
    
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateFareConfiguration(@PathVariable Long id, @RequestBody FareConfigRequest request) {
        // Check admin authorization (bypassed in dev mode)
        if (!isDevMode) {
            User user = getCurrentUser();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        // Check if fare configuration exists
        Optional<FareConfiguration> configOpt = fareConfigurationRepository.findById(id);
        if (configOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        FareConfiguration fareConfig = configOpt.get();
        
        // Update fields if provided
        if (request.getBasePricePerKm() != null) {
            fareConfig.setBasePricePerKm(request.getBasePricePerKm());
        }
        
        if (request.getEffectiveFrom() != null) {
            fareConfig.setEffectiveFrom(request.getEffectiveFrom());
        }
        
        if (request.getEffectiveTo() != null) {
            fareConfig.setEffectiveTo(request.getEffectiveTo());
        }
        
        if (request.getStatus() != null && request.getStatus() != fareConfig.getStatus()) {
            // If changing to active, deactivate all other active configs
            if (request.getStatus() == FareConfiguration.ConfigStatus.ACTIVE) {
                fareConfigurationRepository.findActiveConfig(LocalDateTime.now())
                        .ifPresent(activeConfig -> {
                            if (!activeConfig.getId().equals(id)) {
                                activeConfig.setStatus(FareConfiguration.ConfigStatus.INACTIVE);
                                fareConfigurationRepository.save(activeConfig);
                            }
                        });
            }
            fareConfig.setStatus(request.getStatus());
        }
        
        FareConfiguration updatedConfig = fareConfigurationRepository.save(fareConfig);
        return ResponseEntity.ok(updatedConfig);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFareConfiguration(@PathVariable Long id) {
        // Check admin authorization (bypassed in dev mode)
        if (!isDevMode) {
            User user = getCurrentUser();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        // Check if fare configuration exists
        Optional<FareConfiguration> configOpt = fareConfigurationRepository.findById(id);
        if (configOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Don't allow deleting active configuration
        if (configOpt.get().getStatus() == FareConfiguration.ConfigStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Cannot delete active fare configuration. Deactivate it first."));
        }
        
        fareConfigurationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Fare configuration deleted successfully"));
    }
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
    
    // Request DTO for fare configuration
    static class FareConfigRequest {
        private BigDecimal basePricePerKm;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private FareConfiguration.ConfigStatus status;
        
        public BigDecimal getBasePricePerKm() {
            return basePricePerKm;
        }
        
        public void setBasePricePerKm(BigDecimal basePricePerKm) {
            this.basePricePerKm = basePricePerKm;
        }
        
        public LocalDateTime getEffectiveFrom() {
            return effectiveFrom;
        }
        
        public void setEffectiveFrom(LocalDateTime effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
        }
        
        public LocalDateTime getEffectiveTo() {
            return effectiveTo;
        }
        
        public void setEffectiveTo(LocalDateTime effectiveTo) {
            this.effectiveTo = effectiveTo;
        }
        
        public FareConfiguration.ConfigStatus getStatus() {
            return status;
        }
        
        public void setStatus(FareConfiguration.ConfigStatus status) {
            this.status = status;
        }
    }
} 