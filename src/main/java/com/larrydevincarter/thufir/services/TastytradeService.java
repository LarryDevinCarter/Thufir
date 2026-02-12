package com.larrydevincarter.thufir.services;

import com.larrydevincarter.thufir.models.dtos.OrderRequestDto;
import com.larrydevincarter.thufir.models.entities.Position;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TastytradeService {

    void getAccessToken();

    void forceRefresh();

    Map<String, Object> getCurrentBalances();

    BigDecimal getAvailableCash();

    List<Position> fetchAndSavePositions();

    Map<String, Object> placeOrderDryRun(OrderRequestDto request);

    Map<String, Object> placeOrder(OrderRequestDto request);

    Map<String, BigDecimal> getCurrentMarkPrices(Set<String> equitySymbols, Set<String> cryptoBaseSymbols);
}
