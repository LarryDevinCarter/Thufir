package com.larrydevincarter.thufir.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BuySequenceRecommendation {

    private List<SingleBuyRecommendation> recommendations;
    private String totalToDeploy;
    private String cashRemainingAfter;
    private String message;
    private boolean hasRecommendations;
}