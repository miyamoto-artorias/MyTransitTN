package com.example.mytransittn.config;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.FareConfigurationRepository;
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
    private final FareConfigurationRepository fareConfigRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public DevDataInitializer(UserRepository userRepository, 
                             FareConfigurationRepository fareConfigRepository,
                             BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.fareConfigRepository = fareConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initialize() {
        createUserIfNotExists("admin", "admin@transit.tn", "password", User.ROLE_ADMIN);
        createUserIfNotExists("user", "user@transit.tn", "password", User.ROLE_USER);
        createUserIfNotExists("user1", "user1@gmail.com", "password", User.ROLE_USER);
        createUserIfNotExists("user2", "user2@gmail.com", "password", User.ROLE_USER);
        
        createDefaultFareConfigurationIfNotExists();
    }

    private void createUserIfNotExists(String username, String email, String password, String role) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setBalance(BigDecimal.valueOf(10000.00));
            user.setCreatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            System.out.println("Created test user: " + email);
        }
    }
    
    private void createDefaultFareConfigurationIfNotExists() {
        // Check if any active config exists
        Optional<FareConfiguration> existingConfig = fareConfigRepository.findActiveConfig(LocalDateTime.now());
        
        if (existingConfig.isEmpty()) {
            FareConfiguration config = new FareConfiguration();
            config.setBasePricePerKm(BigDecimal.valueOf(0.5));  // $0.50 per kilometer
            config.setEffectiveFrom(LocalDateTime.now());
            config.setStatus(FareConfiguration.ConfigStatus.ACTIVE);
            
            fareConfigRepository.save(config);
            System.out.println("Created default fare configuration with rate: $0.50 per kilometer");
        }
    }
}