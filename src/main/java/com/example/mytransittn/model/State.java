package com.example.mytransittn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "states")
public class State {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** e.g. 1.0 normal, 1.2 premium region */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal priceMultiplier;


} 