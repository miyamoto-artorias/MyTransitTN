package com.example.mytransittn.controller;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.FareConfigurationRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/fares")
public class FareController {

    private final FareConfigurationRepository fareConfigurationRepository;
    private final UserRepository userRepository;

    @Autowired
    public FareController(FareConfigurationRepository fareConfigurationRepository, UserRepository userRepository) {
        this.fareConfigurationRepository = fareConfigurationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<FareConfiguration> getActiveFareConfiguration() {
        return fareConfigurationRepository.findActiveConfig(LocalDateTime.now())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FareConfiguration>> getAllFareConfigurations() {
        // Only admins can view all fare configs
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(fareConfigurationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareConfiguration> getFareConfigurationById(@PathVariable Long id) {
        // Only admins can view specific fare configs
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        return fareConfigurationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
} 