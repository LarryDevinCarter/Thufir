package com.larrydevincarter.thufir.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TastytradeClient {

    private final RestTemplate restTemplate;

    @Value("${tastytrade.api.base-url:https://api.tastyworks.com}")
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
}