package com.larrydevincarter.thufir.models.dtos;

import com.larrydevincarter.thufir.models.OrderLeg;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderRequestDto {
    private String timeInForce;
    private String orderType;
    private BigDecimal limitPrice;
    private String priceEffect;
    private List<OrderLeg> legs;
}
