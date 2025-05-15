package com.example.mytransittn.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
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
    @JsonIgnoreProperties("lines")
    private List<Station> stations = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Line line = (Line) o;
        return Objects.equals(id, line.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Line{" +
               "id=" + id +
               ", code='" + code + '\'' +
               ", fareMultiplier=" + fareMultiplier +
               '}';
    }
} 