
package com.larrydevincarter.thufir.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.models.dtos.OptionBatchRequestDto;
import com.larrydevincarter.thufir.models.dtos.OptionBatchResponseDto;
import com.larrydevincarter.thufir.models.dtos.StockCandidatesRequestDto;
import com.larrydevincarter.thufir.services.OptionScannerUpdateMonitor;
import com.larrydevincarter.thufir.utils.OptionScannerClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OptionScannerClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OptionScannerUpdateMonitor updateMonitor;

    private static final String BASE_URL = "http://localhost:8081/api";

    public void waitForUpdateIfNeeded() {
        if (updateMonitor.isPotentiallyUpdating()) {
            OptionScannerClientUtils.waitForUpdateComplete(updateMonitor);
        }
    }

    /**
     * Fetch ranked stock candidates for new cash-secured puts (GET endpoint).
     * Filters applied server-side based on params.
     *
     * @return List of ticker strings from OptionScanner response
     */
    public List<String> getStockCandidates(StockCandidatesRequestDto requestDto) {
        waitForUpdateIfNeeded();
        String url = BASE_URL + "/stock-candidates";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<StockCandidatesRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Stock candidates failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Stock candidates error", e);
            throw new RuntimeException("Stock candidates error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch batch option chains with pre-fetched prices (POST endpoint).
     * Body: List of DTOs like [{"ticker": "AAPL", "currentPrice": 225.50}, ...]
     *
     * @return Raw data map from OptionScanner response
     */
    public OptionBatchResponseDto getBatchOptionChains(List<OptionBatchRequestDto> dtos) {
        waitForUpdateIfNeeded();
        String url = BASE_URL + "/option-chains/batch";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<OptionBatchRequestDto>> entity = new HttpEntity<>(dtos, headers);

        try {
            ResponseEntity<OptionBatchResponseDto> response = restTemplate.exchange(url, HttpMethod.POST, entity, OptionBatchResponseDto.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Batch chains failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Batch chains error", e);
            throw new RuntimeException("Batch chains error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch batch covered call candidates with pre-fetched prices (POST endpoint).
     * Similar to batch chains, but filtered for calls.
     * Body: List of DTOs like [{"ticker": "AAPL", "currentPrice": 225.50}, ...]
     *
     * @return Raw data map from OptionScanner response
     */
    public Map<String, Object> getBatchCoveredCallCandidates(List<OptionBatchRequestDto> dtos) {
        waitForUpdateIfNeeded();
        String url = BASE_URL + "/covered-calls/batch";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<OptionBatchRequestDto>> entity = new HttpEntity<>(dtos, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Batch covered calls failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Batch covered calls error", e);
            throw new RuntimeException("Batch covered calls error: " + e.getMessage(), e);
        }
    }
}