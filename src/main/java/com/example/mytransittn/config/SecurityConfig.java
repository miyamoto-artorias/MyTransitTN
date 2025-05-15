package com.example.mytransittn.config;


import com.example.mytransittn.security.JwtAuthenticationFilter;
import com.example.mytransittn.service.CustomOAuth2UserService;
import com.example.mytransittn.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    // ********** DEVELOPMENT MODE FLAG **********
    // Set to false in production to restore security
    private static final boolean DEVELOPMENT_MODE = true;
    // ******************************************

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, @Lazy CustomOAuth2UserService customOAuth2UserService) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (DEVELOPMENT_MODE) {
            // Development mode configuration - completely disable security
            http.cors(withDefaults())
                .csrf(csrf -> csrf.disable())  // Disable CSRF protection in dev mode
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()  // Allow all requests without authentication
                )
                .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .defaultSuccessUrl("http://localhost:4200/dashboard", true)
                );
                
            // Still add JWT filter to process tokens in Swagger API calls
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            // Production mode configuration - full security with JWT
            http.cors(withDefaults())
                .csrf(csrf -> csrf.disable()) // JWT is stateless, so CSRF is not needed
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Set to STATELESS for JWT
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/check", "/api/auth/login", "/api/auth/register", 
                        "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/continue-with-email",
                        "/swagger-ui-custom.html", "/swagger-ui.html", "/swagger-ui/**", 
                        "/v3/api-docs/**", "/webjars/**").permitAll()
                    .anyRequest().hasAnyRole("ADMIN", "USER")
                )
                .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .defaultSuccessUrl("http://localhost:4200/dashboard", true)
                )
                .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(HttpServletResponse.SC_OK);
                    })
                    .invalidateHttpSession(true)
                    .permitAll()
                )
                .exceptionHandling(exception -> exception
                    .authenticationEntryPoint((request, response, ex) -> {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    })
                );
                
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // ********** DEVELOPMENT ONLY - MODIFY IN PRODUCTION **********
        // In production, remove http://localhost:8080 if not needed
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:8080"));
        // ********************************************************
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}