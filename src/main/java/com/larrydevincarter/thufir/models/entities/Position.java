package com.larrydevincarter.thufir.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "position")
@Data
@NoArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal averagePrice;

    @Column(nullable = false)
    private BigDecimal marketValue;

    @Column(nullable = false)
    private BigDecimal unrealizedPnl;

    @Column(nullable = false)
    private LocalDateTime snapshotAt;

    @PrePersist
    protected void onCreate() {
        snapshotAt = LocalDateTime.now();
    }
}