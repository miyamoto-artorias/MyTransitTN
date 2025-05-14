package com.example.mytransittn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "stations")
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(optional = false)
    private State state;

    /** back-ref to lines this station sits on */
    @ManyToMany(mappedBy = "stations")
    @JsonIgnoreProperties("stations")
    private Set<Line> lines = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private StationStatus status;

    public enum StationStatus { OPEN, CLOSED, UNDER_MAINTENANCE }
    

} 