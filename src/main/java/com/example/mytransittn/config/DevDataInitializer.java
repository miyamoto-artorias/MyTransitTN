package com.example.mytransittn.config;

import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Initializes development data
 * Only active when "dev" profile is active
 */
@Configuration
@Profile("dev")
public class DevDataInitializer {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public DevDataInitializer(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initialize() {
        createUserIfNotExists("admin", "admin@transit.tn", "password", User.ROLE_ADMIN);
        createUserIfNotExists("user", "user@transit.tn", "password", User.ROLE_USER);
        createUserIfNotExists("user1", "user1@gmail.com", "password", User.ROLE_USER);
        createUserIfNotExists("user2", "user2@gmail.com", "password", User.ROLE_USER);
    }

    private void createUserIfNotExists(String username, String email, String password, String role) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setBalance(BigDecimal.valueOf(100.00));
            user.setCreatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            System.out.println("Created test user: " + email);
        }
    }
} 