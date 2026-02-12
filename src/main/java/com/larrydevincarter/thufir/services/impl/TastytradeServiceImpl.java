package com.larrydevincarter.thufir.services.impl;

import com.larrydevincarter.thufir.clients.TastytradeClient;
import com.larrydevincarter.thufir.models.dtos.OrderRequestDto;
import com.larrydevincarter.thufir.models.entities.Position;
import com.larrydevincarter.thufir.repositories.PositionRepository;
import com.larrydevincarter.thufir.services.TastytradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TastytradeServiceImpl implements TastytradeService {

    private final TastytradeClient tastytradeClient;
    private final PositionRepository positionRepository;

    @Value("${tastytrade.live.account-number}")
    private String configuredAccountNumber;
    @Value("${tastytrade.oauth.client-id}")
    private String clientId;

    @Value("${tastytrade.oauth.client-secret}")
    private String clientSecret;

    @Value("${tastytrade.oauth.refresh-token}")
    private String refreshToken;

    private String accessToken;
    private LocalDateTime tokenExpiry;

    @Override
    public void getAccessToken() {
        if (accessToken == null || LocalDateTime.now().isAfter(tokenExpiry.minusMinutes(2))) {
            refreshAccessToken();
        }
    }

    private void refreshAccessToken() {
        try {
            Map<String, Object> tokenResponse = tastytradeClient.refreshToken(clientId, clientSecret, refreshToken);

            accessToken = (String) tokenResponse.get("access_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");  // usually 900
            tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 900);

            log.info("Tastytrade access token refreshed, expires at {}", tokenExpiry);
        } catch (Exception e) {
            log.error("Failed to refresh Tastytrade access token", e);
            throw new RuntimeException("Tastytrade authentication failure", e);
        }
    }

    @Override
    public void forceRefresh() {
        refreshAccessToken();
    }

    @Override
    public Map<String, Object> getCurrentBalances() {
        getAccessToken();

        if (configuredAccountNumber == null) {
            throw new IllegalStateException("No account number available");
        }
        return tastytradeClient.getBalances(accessToken, configuredAccountNumber);
    }

    @Override
    public BigDecimal getAvailableCash() {
        Map<String, Object> balances = getCurrentBalances();
        return parseBigDecimal(balances.get("equity-buying-power"));
    }

    @Override
    public List<Position> fetchAndSavePositions() {
        getAccessToken();
        if (configuredAccountNumber == null) {
            throw new IllegalStateException("No account number available");
        }

        List<Map<String, Object>> rawPositions = tastytradeClient.getPositions(accessToken, configuredAccountNumber);
        List<Position> positions = new ArrayList<>();

        for (Map<String, Object> raw : rawPositions) {
            Position pos = mapToPosition(raw);
            if (pos != null) {
                positions.add(positionRepository.save(pos));
            }
        }

        log.info("Fetched and saved {} positions", positions.size());
        return positions;
    }

    private Position mapToPosition(Map<String, Object> raw) {
        try {
            Position pos = new Position();
            pos.setSymbol((String) raw.get("symbol"));
            pos.setQuantity(parseBigDecimal(raw.get("quantity")));
            pos.setAveragePrice(parseBigDecimal(raw.get("average-open-price")));
            //TODO: Needs updated with second pull
            pos.setMarketValue(BigDecimal.ZERO);
            pos.setUnrealizedPnl(BigDecimal.ZERO);
            return pos;
        } catch (Exception e) {
            log.warn("Failed to map position: {}", raw, e);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof String s) return new BigDecimal(s.trim());
        if (obj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, Object> placeOrderDryRun(OrderRequestDto request) {
        getAccessToken();
        Map<String, Object> body = buildOrderBody(request);
        return tastytradeClient.submitOrder(accessToken, configuredAccountNumber, body, true);
    }

    @Override
    public Map<String, Object> placeOrder(OrderRequestDto request) {
        getAccessToken();
        Map<String, Object> body = buildOrderBody(request);
        Map<String, Object> result = tastytradeClient.submitOrder(accessToken, configuredAccountNumber, body, false);
        log.info("Order placed: {}", result);
        return result;
    }

    private Map<String, Object> buildOrderBody(OrderRequestDto req) {
        Map<String, Object> body = new HashMap<>();
        body.put("time-in-force", req.getTimeInForce() != null ? req.getTimeInForce() : "Day");
        body.put("order-type", req.getOrderType());

        if (req.getLimitPrice() != null) {
            body.put("price", req.getLimitPrice().toPlainString());
            body.put("price-effect", req.getPriceEffect() != null ? req.getPriceEffect() : "Debit");
        }

        List<Map<String, Object>> legs = req.getLegs().stream().map(leg -> {
            Map<String, Object> legMap = new HashMap<>();
            legMap.put("instrument-type", leg.getInstrumentType());
            legMap.put("symbol", leg.getSymbol());
            legMap.put("quantity", leg.getQuantity().toPlainString());
            legMap.put("action", leg.getAction());
            return legMap;
        }).toList();

        body.put("legs", legs);
        return body;
    }
}