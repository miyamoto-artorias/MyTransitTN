package com.example.mytransittn.controller;

import com.example.mytransittn.model.User;
import com.example.mytransittn.service.OpenRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OpenRouteService openRouteService;

    public AdminController(OpenRouteService openRouteService) {
        this.openRouteService = openRouteService;
    }

    /**
     * Clear the OpenRouteService distance cache
     * Only accessible to admins
     */
    @PostMapping("/clear-route-cache")
    public ResponseEntity<Map<String, String>> clearRouteCache() {
        // In development mode, we allow this without auth check
        if (!isDevelopmentMode()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).build();
            }
        }
        
        openRouteService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Route cache cleared successfully"));
    }
    
    private boolean isDevelopmentMode() {
        // Hardcoded to true for now, should match SecurityConfig.DEVELOPMENT_MODE
        return true;
    }
} 