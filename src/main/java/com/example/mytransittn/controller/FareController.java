package com.example.mytransittn.controller;

import com.example.mytransittn.dto.FareConfigurationDto;
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
import java.util.stream.Collectors;

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
    public ResponseEntity<FareConfigurationDto> getActiveFareConfiguration() {
        return fareConfigurationRepository.findActiveConfig(LocalDateTime.now())
                .map(FareConfigurationDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FareConfigurationDto>> getAllFareConfigurations() {
        // Only admins can view all fare configs
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        List<FareConfigurationDto> dtos = fareConfigurationRepository.findAll().stream()
                .map(FareConfigurationDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareConfigurationDto> getFareConfigurationById(@PathVariable Long id) {
        // Only admins can view specific fare configs
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        return fareConfigurationRepository.findById(id)
                .map(FareConfigurationDto::fromEntity)
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