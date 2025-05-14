package com.example.mytransittn.controller;

import com.example.mytransittn.model.State;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.StateRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/states")
public class StateAdminController {

    private final StateRepository stateRepository;
    private final UserRepository userRepository;
    private final boolean isDevMode = true; // Set to false in production

    @Autowired
    public StateAdminController(StateRepository stateRepository, UserRepository userRepository) {
        this.stateRepository = stateRepository;
        this.userRepository = userRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> createState(@RequestBody StateRequest request) {
        // Check admin authorization (bypassed in dev mode)
        if (!isDevMode) {
            User user = getCurrentUser();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        // Validate request
        if (request.getName() == null || request.getPriceMultiplier() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }
        
        // Check if state name is already in use
        if (stateRepository.findByName(request.getName()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "State name already exists"));
        }
        
        // Create new state
        State state = new State();
        state.setName(request.getName());
        state.setPriceMultiplier(request.getPriceMultiplier());
        
        State savedState = stateRepository.save(state);
        return ResponseEntity.ok(savedState);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateState(@PathVariable Long id, @RequestBody StateRequest request) {
        // Check admin authorization (bypassed in dev mode)
        if (!isDevMode) {
            User user = getCurrentUser();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        // Check if state exists
        Optional<State> stateOpt = stateRepository.findById(id);
        if (stateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        State state = stateOpt.get();
        
        // Update fields if provided
        if (request.getName() != null) {
            // Check if another state already uses this name
            Optional<State> existingState = stateRepository.findByName(request.getName());
            if (existingState.isPresent() && !existingState.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "State name already exists"));
            }
            state.setName(request.getName());
        }
        
        if (request.getPriceMultiplier() != null) {
            state.setPriceMultiplier(request.getPriceMultiplier());
        }
        
        State updatedState = stateRepository.save(state);
        return ResponseEntity.ok(updatedState);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteState(@PathVariable Long id) {
        // Check admin authorization (bypassed in dev mode)
        if (!isDevMode) {
            User user = getCurrentUser();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        // Check if state exists
        if (!stateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Note: Should check if any stations are using this state before deletion
        // For simplicity, we're not implementing that check here
        
        stateRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "State deleted successfully"));
    }
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
    
    // Request DTO for state creation/update
    static class StateRequest {
        private String name;
        private BigDecimal priceMultiplier;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public BigDecimal getPriceMultiplier() {
            return priceMultiplier;
        }
        
        public void setPriceMultiplier(BigDecimal priceMultiplier) {
            this.priceMultiplier = priceMultiplier;
        }
    }
} 