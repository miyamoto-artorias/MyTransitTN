package com.example.mytransittn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "lines")
public class Line {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "M1", "Blue" */
    @Column(nullable = false, unique = true)
    private String code;

    /** pricing & UI tier */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal fareMultiplier;

    /**
     * ordered list of stations along the route.
     * station_order column in join table preserves the sequence.
     */
    @ManyToMany
    @JoinTable(
            name = "line_stations",
            joinColumns = @JoinColumn(name = "line_id"),
            inverseJoinColumns = @JoinColumn(name = "station_id")
    )
    @OrderColumn(name = "station_order")
    private List<Station> stations = new ArrayList<>();


} 