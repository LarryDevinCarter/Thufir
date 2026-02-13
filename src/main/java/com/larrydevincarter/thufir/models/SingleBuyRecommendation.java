package com.larrydevincarter.thufir.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SingleBuyRecommendation {
    private String category;
    private String symbol;
    private String quantity;
    private String priceUsed;
    private String estCost;
    private boolean isCrypto;
}
