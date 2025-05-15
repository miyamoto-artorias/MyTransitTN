package com.example.mytransittn.controller;


import com.example.mytransittn.model.PasswordResetToken;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.PasswordResetTokenRepository;
import com.example.mytransittn.repository.UserRepository;
import com.example.mytransittn.security.JwtUtils;
import com.example.mytransittn.service.EmailService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthController(UserRepository userRepository,
                          BCryptPasswordEncoder encoder,
                          PasswordResetTokenRepository tokenRepository,
                          EmailService emailService,
                          AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        String email = loginRequest.getEmail().toLowerCase().trim();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            // Return the token in the response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Connexion réussie");
            responseBody.put("token", jwt);
            
            return ResponseEntity.ok().body(responseBody);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Email ou mot de passe invalide"));
        }
    }

    @GetMapping("/check")
    public Map<String, Boolean> checkAuthentication(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        return Collections.singletonMap("authenticated", isAuthenticated);
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> processForgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<User> optional = userRepository.findByEmail(email);
        if (optional.isPresent()) {
            User user = optional.get();
            tokenRepository.deleteByUser(user);
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().plusHours(1));
            tokenRepository.save(resetToken);

            String resetLink = "http://localhost:4200/reset-password?token=" + token;
            emailService.sendEmail(email, "Demande de réinitialisation de mot de passe", "Lien : " + resetLink);
        }
        return ResponseEntity.ok().body(Collections.singletonMap("message", "Lien de réinitialisation envoyé si l'email existe"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> processReset(@RequestBody ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(
                    Collections.singletonMap("error", "Les mots de passe ne correspondent pas")
            );
        }

        if (!request.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            return ResponseEntity.badRequest().body(
                    Collections.singletonMap("error",
                            "Le mot de passe doit contenir au moins 8 caractères avec 1 majuscule, 1 minuscule et 1 chiffre")
            );
        }

        Optional<PasswordResetToken> optionalToken = tokenRepository.findByToken(request.getToken());
        if (optionalToken.isEmpty() || optionalToken.get().isExpired()) {
            return ResponseEntity.badRequest().body(
                    Collections.singletonMap("error", "Jeton invalide ou expiré")
            );
        }

        PasswordResetToken resetToken = optionalToken.get();
        User user = resetToken.getUser();
        user.setPassword(encoder.encode(request.getPassword()));
        userRepository.save(user);
        tokenRepository.delete(resetToken);

        return ResponseEntity.ok().body(
                Collections.singletonMap("message", "Mot de passe réinitialisé avec succès")
        );
    }

    static class LoginRequest {
        private String email;
        private String password;
        @JsonProperty("remember-me")
        private boolean rememberMe;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isRememberMe() { return rememberMe; }
        public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }
    }

    static class ResetPasswordRequest {
        private String token;
        private String password;
        private String confirmPassword;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}