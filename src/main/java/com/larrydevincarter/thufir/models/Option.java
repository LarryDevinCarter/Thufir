package com.larrydevincarter.thufir.models;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Option {

    private String id;
    private String symbol;
    private String underlyingSymbol;
    private LocalDate expirationDate;
    private Double strike;
    private String optionType;
    private Double previousClose;
    private boolean tradedPreviousDay;
    private Double adjustedPe;
    private Double yield;
    private LocalDateTime lastUpdated;
    private Double delta;
}