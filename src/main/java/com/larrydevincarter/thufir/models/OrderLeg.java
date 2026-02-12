package com.larrydevincarter.thufir.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderLeg {
    private String instrumentType;
    private String symbol;
    private BigDecimal quantity;
    private String action;
}
