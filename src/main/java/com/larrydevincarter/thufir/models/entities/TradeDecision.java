package com.larrydevincarter.thufir.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_decisions")
@Data
@NoArgsConstructor
public class TradeDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 20)
    private String ticker;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    @Setter
    @Column
    private Double probabilitySuccess;

    @Setter
    @Getter
    @Column(length = 20)
    private String expectedReturn;
}