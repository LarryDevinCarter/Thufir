package com.larrydevincarter.thufir.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
@Slf4j
public class TastytradeClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tastytrade.sandbox.base-url}")
    private String baseUrl;

    @Value("${tastytrade.sandbox.username}")
    private String username;

    @Value("${tastytrade.sandbox.password}")
    private String password;

    @Value("${tastytrade.sandbox.account-number}")
    private String accountNumber;

    private String sessionToken;
    private long tokenExpirationTimeMs = 0;

    public TastytradeClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            refreshSessionIfNeeded();
            log.info("TastytradeClient initialized. Sandbox account: {}", accountNumber);
        } catch (Exception e) {
            log.error("Tastytrade sandbox login failed during startup. Trading will be blocked until fixed.", e);
        }
    }

    private synchronized void refreshSessionIfNeeded() {
        if (sessionToken == null || System.currentTimeMillis() > tokenExpirationTimeMs) {
            login();
        }
    }

    private void login() {
        String url = baseUrl + "/sessions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "login", username,
                "password", password,
                "remember-me", "true"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                sessionToken = (String) data.get("session-token");

                tokenExpirationTimeMs = System.currentTimeMillis() + (12 * 60 * 60 * 1000);

                log.info("Tastytrade sandbox session established successfully for account {}", accountNumber);
            } else {
                throw new RuntimeException("Unexpected response from login: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Tastytrade login failed (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Tastytrade sandbox authentication failed - check username/password", e);
        } catch (Exception e) {
            log.error("Unexpected error during Tastytrade login", e);
            throw new RuntimeException("Tastytrade login exception", e);
        }
    }

    private HttpHeaders getAuthHeaders() {
        refreshSessionIfNeeded();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", sessionToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public Map<String, Object> getAccountBalances() {
        String url = baseUrl + "/accounts/" + accountNumber + "/balances";

        HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return (Map<String, Object>) response.getBody().get("data");
            }
            throw new RuntimeException("Balances fetch failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to fetch balances for account {}: {}", accountNumber, e.getMessage());
            throw new RuntimeException("Balances API error", e);
        }
    }

    public Map<String, Object> getPositions() {
        String url = baseUrl + "/accounts/" + accountNumber + "/positions?include=underlyings";

        HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return (Map<String, Object>) response.getBody().get("data");
            }
            throw new RuntimeException("Positions fetch failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to fetch positions for account {}: {}", accountNumber, e.getMessage());
            throw new RuntimeException("Positions API error", e);
        }
    }

    /**
     * Places a single-leg option order (used for selling puts/calls in the wheel).
     * orderDetails should follow Tastytrade's order JSON structure.
     */
    public Map<String, Object> placeOrder(Map<String, Object> orderDetails) {
        String url = baseUrl + "/accounts/" + accountNumber + "/orders";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderDetails, getAuthHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.ACCEPTED) {
                log.info("Order submitted successfully to Tastytrade sandbox: {}", response.getBody());
                return (Map<String, Object>) response.getBody().get("data");
            } else {
                log.error("Order placement rejected: HTTP {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Order placement failed: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception placing order: {}", e.getMessage(), e);
            throw new RuntimeException("Order execution exception", e);
        }
    }

    public boolean isSessionValid() {
        return sessionToken != null && System.currentTimeMillis() < tokenExpirationTimeMs;
    }
}