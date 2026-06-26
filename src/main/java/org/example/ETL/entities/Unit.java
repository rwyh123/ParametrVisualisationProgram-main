package org.example.ETL.entities;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(
        name = "unit",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_unit_short_name",
                columnNames = "short_name"
        )
)
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 30, nullable = false)
    private String fullName;

    @Column(name = "short_name", length = 10)
    private String shortName;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Indicator> indicators;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
