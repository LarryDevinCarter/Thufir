package com.larrydevincarter.thufir.models;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Asset {

    private String id;
    private String symbol;
    private String name;
    private String exchange;
    private String status;
    private boolean tradable;
    private Double currentPrice;
    private LocalDateTime lastPriceUpdated;
    private LocalDateTime lastUpdated;
    private Double adjustedNetIncome;
    private Double adjustedEarningsPerShare;
    private Double sharesOutstanding;
    private Double marketCap;
}
