package com.example.mytransittn.controller;

import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class EmailAuthController {

    private final UserRepository userRepository;

    @Autowired
    public EmailAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/continue-with-email")
    public ResponseEntity<?> continueWithEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Email existe , procede login"));
        }
        return ResponseEntity.status(404).body("Emailm'existe pas ");
    }
}