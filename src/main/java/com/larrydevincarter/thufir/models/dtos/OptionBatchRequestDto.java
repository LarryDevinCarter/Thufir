package com.larrydevincarter.thufir.models.dtos;

import lombok.Data;

@Data
public class OptionBatchRequestDto {
    private String ticker;
    private Double currentPrice;
    private Double costBasis;
}