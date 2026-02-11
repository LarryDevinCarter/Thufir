package com.larrydevincarter.thufir.services.impl;

import com.larrydevincarter.thufir.clients.TastytradeClient;
import com.larrydevincarter.thufir.services.TastytradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TastytradeServiceImpl implements TastytradeService {

    private final TastytradeClient tastytradeClient;

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

    public Map<String, Object> getCurrentBalances() {
        getAccessToken();

        if (configuredAccountNumber == null) {
            throw new IllegalStateException("No account number available");
        }
        return tastytradeClient.getBalances(accessToken, configuredAccountNumber);
    }

    public BigDecimal getAvailableCash() {
        Map<String, Object> balances = getCurrentBalances();
        return parseMoney(balances.get("cash-balance"), "cash-balance");
    }

    private BigDecimal parseMoney(Object obj, String fieldName) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                log.error("Invalid {} value: {}", fieldName, s);
                return BigDecimal.ZERO;
            }
        }
        if (obj instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        log.warn("Unexpected type for {}: {}", fieldName, obj.getClass().getName());
        return BigDecimal.ZERO;
    }
}