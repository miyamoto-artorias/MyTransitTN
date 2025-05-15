package com.example.mytransittn.config;

import com.example.mytransittn.model.FareConfiguration;
import com.example.mytransittn.model.Line;
import com.example.mytransittn.model.State;
import com.example.mytransittn.model.Station;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.FareConfigurationRepository;
import com.example.mytransittn.repository.LineRepository;
import com.example.mytransittn.repository.StateRepository;
import com.example.mytransittn.repository.StationRepository;
import com.example.mytransittn.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final StationRepository stationRepository;
    private final StateRepository stateRepository;
    private final LineRepository lineRepository;

    @Autowired
    public DevDataInitializer(UserRepository userRepository, 
                             FareConfigurationRepository fareConfigRepository,
                             BCryptPasswordEncoder passwordEncoder,
                             StationRepository stationRepository,
                             StateRepository stateRepository,
                             LineRepository lineRepository) {
        this.userRepository = userRepository;
        this.fareConfigRepository = fareConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.stationRepository = stationRepository;
        this.stateRepository = stateRepository;
        this.lineRepository = lineRepository;
    }

    @PostConstruct
    public void initialize() {
        createUserIfNotExists("admin", "admin@transit.tn", "password", User.ROLE_ADMIN);
        createUserIfNotExists("user", "user@transit.tn", "password", User.ROLE_USER);
        createUserIfNotExists("user1", "user1@gmail.com", "password", User.ROLE_USER);
        createUserIfNotExists("user2", "user2@gmail.com", "password", User.ROLE_USER);
        
        createDefaultFareConfigurationIfNotExists();
        
        // Create default state before stations
        State defaultState = createDefaultStateIfNotExists();
        
        createStationsIfNotExist(defaultState);
        
        // Create transit lines after stations are created
        createTransitLinesIfNotExist();
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
    
    private State createDefaultStateIfNotExists() {
        // Create a default state for stations if it doesn't exist
        Optional<State> existingState = stateRepository.findByName("Tunisia");
        
        if (existingState.isPresent()) {
            return existingState.get();
        }
        
        State state = new State();
        state.setName("Tunisia");
        state.setPriceMultiplier(BigDecimal.valueOf(1.0)); // Normal pricing
        
        State savedState = stateRepository.save(state);
        System.out.println("Created default state: Tunisia");
        return savedState;
    }
    
    private void createStationsIfNotExist(State defaultState) {
        // Create stations if they don't exist
        createStationIfNotExists("Tunis", 10.090942, 36.708064, defaultState);
        createStationIfNotExists("Binzert", 9.728394, 36.980615, defaultState);
        createStationIfNotExists("Nabeel", 10.750122, 36.743286, defaultState);
        createStationIfNotExists("Zaghouan", 10.030518, 36.284135, defaultState);
        createStationIfNotExists("Soussa", 10.442505, 35.889050, defaultState);
        createStationIfNotExists("Mahdia", 10.936890, 35.505400, defaultState);
        createStationIfNotExists("Sfax", 10.717163, 34.786739, defaultState);
        createStationIfNotExists("Qurwen", 9.843750, 35.550105, defaultState);
        createStationIfNotExists("Sidi Bouzide", 9.470215, 34.777716, defaultState);
        
        System.out.println("Initialized stations");
    }
    
    private void createStationIfNotExists(String name, double longitude, double latitude, State state) {
        if (stationRepository.findByName(name).isEmpty()) {
            Station station = new Station();
            station.setName(name);
            station.setLongitude(longitude);
            station.setLatitude(latitude);
            station.setState(state);
            station.setStatus(Station.StationStatus.OPEN);
            
            stationRepository.save(station);
            System.out.println("Created station: " + name);
        }
    }
    
    private void createTransitLinesIfNotExist() {
        // Check if lines already exist
        if (lineRepository.findByCode("line1").isPresent() &&
            lineRepository.findByCode("line2").isPresent() &&
            lineRepository.findByCode("line3").isPresent()) {
            return;
        }

        // Line 1: Tunis -> Binzert -> Nabeel -> Zaghouan
        createLineIfNotExists("line1", 
            List.of("Tunis", "Binzert", "Nabeel", "Zaghouan"));
        
        // Line 2: Zaghouan -> Soussa -> Mahdia
        createLineIfNotExists("line2", 
            List.of("Zaghouan", "Soussa", "Mahdia"));
        
        // Line 3: Mahdia -> Sfax -> Qurwen -> Sidi Bouzide
        createLineIfNotExists("line3", 
            List.of("Mahdia", "Sfax", "Qurwen", "Sidi Bouzide"));
        
        System.out.println("Initialized transit lines");
    }
    
    private void createLineIfNotExists(String code, List<String> stationNames) {
        // Skip if line already exists
        if (lineRepository.findByCode(code).isPresent()) {
            System.out.println("Line " + code + " already exists, skipping");
            return;
        }
        
        Line line = new Line();
        line.setCode(code);
        line.setFareMultiplier(BigDecimal.valueOf(1.0)); // Standard fare
        
        // Get stations by name
        List<Station> stationsForLine = new ArrayList<>();
        for (String stationName : stationNames) {
            Optional<Station> stationOpt = stationRepository.findByName(stationName);
            if (stationOpt.isPresent()) {
                stationsForLine.add(stationOpt.get());
            } else {
                System.out.println("Warning: Station " + stationName + " not found for line " + code);
            }
        }
        
        line.setStations(stationsForLine);
        
        lineRepository.save(line);
        System.out.println("Created line: " + code + " with " + stationsForLine.size() + " stations");
    }
}