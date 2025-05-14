package com.example.mytransittn.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom d'utilisateur est requis")
    @Column(unique = true)
    private String username;

    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @NotBlank(message = "Le mot de passe est requis")
    private String password;

    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est requis")
    @Column(unique = true)
    private String email;

    private String role = ROLE_USER;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user")
    private Set<Journey> journeys = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Payment> payments = new HashSet<>();


}