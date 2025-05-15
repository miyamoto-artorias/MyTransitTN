package com.example.mytransittn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return Objects.equals(id, station.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Station{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", latitude=" + latitude +
               ", longitude=" + longitude +
               ", status=" + status +
               '}';
    }
} 