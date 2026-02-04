package com.larrydevincarter.thufir.clients;

import com.larrydevincarter.thufir.models.dtos.MarketStatusDto;
import com.larrydevincarter.thufir.models.dtos.UpdateStatusDto;
import com.larrydevincarter.thufir.services.OptionScannerUpdateMonitor;
import com.larrydevincarter.thufir.utils.OptionScannerClientUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketStatusClient {

    private static final Logger log = LoggerFactory.getLogger(MarketStatusClient.class);

    private final RestTemplate restTemplate;
    private final OptionScannerUpdateMonitor updateMonitor;

    private final String endpointUrl = "http://localhost:8081/api/market-status";

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 10000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public MarketStatusDto getStatus() {
        if (updateMonitor.isPotentiallyUpdating()) {
            OptionScannerClientUtils.waitForUpdateComplete(updateMonitor);
        }

        return fetchMarketStatusWithRetry();
    }

    private MarketStatusDto fetchMarketStatusWithRetry() {
        int attempt = 0;
        long delayMs = INITIAL_RETRY_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                ResponseEntity<MarketStatusDto> response = restTemplate.exchange(
                        endpointUrl,
                        HttpMethod.GET,
                        null,
                        MarketStatusDto.class
                );

                HttpStatusCode statusCode = response.getStatusCode();
                if (statusCode == HttpStatus.OK && response.getBody() != null) {
                    log.info("Successfully fetched market status from OptionScanner on attempt {} (200 OK)", attempt);
                    return response.getBody();
                } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
                    log.warn("OptionScanner returned 503 on attempt {} — retrying in {} ms", attempt, delayMs);
                } else {
                    log.warn("Unexpected status {} from OptionScanner on attempt {} — retrying in {} ms", statusCode, attempt, delayMs);
                }

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    log.warn("OptionScanner 503 on attempt {} — retrying in {} ms", attempt, delayMs);
                } else {
                    log.warn("HTTP error {} on attempt {} — retrying in {} ms", e.getStatusCode(), attempt, delayMs);
                }
            } catch (Exception e) {
                log.warn("Failed to reach OptionScanner on attempt {} ({}: {}) — retrying in {} ms",
                        attempt, e.getClass().getSimpleName(), e.getMessage(), delayMs);
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry sleep interrupted");
                    throw new RuntimeException("Interrupted while retrying market status fetch", ie);
                }
                delayMs = (long) (delayMs * BACKOFF_MULTIPLIER);
            }
        }

        log.error("All {} retry attempts failed — assuming NOT a trading day to prioritize safety", MAX_RETRIES);
        MarketStatusDto emergency = new MarketStatusDto();
        emergency.setTradingDay(false);
        emergency.setTodayCloseTime("15:00");
        return emergency;
    }

    public LocalTime parseCloseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            log.warn("Invalid close time string — defaulting to 15:00");
            return LocalTime.of(15, 0);
        }
        return LocalTime.parse(timeStr);
    }
}