package com.larrydevincarter.thufir.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TastytradeClient {

    private final RestTemplate restTemplate;

    @Value("${tastytrade.live.base-url}")
    private String baseUrl;

    public Map<String, Object> refreshToken(String clientId, String clientSecret, String refreshToken) {
        String url = baseUrl + "/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("Token refresh successful");
            return response.getBody();
        } else {
            log.error("Token refresh failed: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Tastytrade token refresh failed: " + response.getStatusCode());
        }
    }

    public Map<String, Object> getBalances(String accessToken, String accountNumber) {
        String url = baseUrl + "/accounts/" + accountNumber + "/balances";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data != null) {
                return data;
            }
        }
        log.error("Failed to fetch balances for account {}: status={}", accountNumber, response.getStatusCode());
        throw new RuntimeException("Failed to fetch tastytrade balances for account " + accountNumber);
    }

    public List<Map<String, Object>> getPositions(String accessToken, String accountNumber) {
        String url = baseUrl + "/accounts/" + accountNumber + "/positions?include-marks=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");
            if (dataMap != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) dataMap.get("items");
                if (items != null) {
                    return items;
                }
            }
        }
        log.error("Failed to fetch positions for account {}: status={}", accountNumber, response.getStatusCode());
        throw new RuntimeException("Failed to fetch tastytrade positions for account " + accountNumber);
    }

    public Map<String, Object> submitOrder(String accessToken, String accountNumber, Map<String, Object> orderBody, boolean dryRun) {
        String endpoint = dryRun ? "/orders/dry-run" : "/orders";
        String url = baseUrl + "/accounts/" + accountNumber + endpoint;
        log.info(url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            log.error("Order submission failed (dryRun={}): status={}, body={}", dryRun, response.getStatusCode(), response.getBody());
            throw new RuntimeException("Order submission failed: " + response.getStatusCode());
        }
    }

    public Map<String, Object> getMarketData(String accessToken, Map<String, String> typeToSymbols) {
        String url = baseUrl + "/market-data/by-type";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        typeToSymbols.forEach((type, symbolsCsv) ->
                builder.queryParam(type, symbolsCsv)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info(builder.toUriString());

        ResponseEntity<Map> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            return data != null ? data : new HashMap<>();
        }

        log.error("Market data fetch failed: status = {}", response.getStatusCode());
        throw new RuntimeException("Failed to fetch market data from tastytrade");
    }
}