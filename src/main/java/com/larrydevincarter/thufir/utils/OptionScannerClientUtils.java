package com.larrydevincarter.thufir.utils;

import com.larrydevincarter.thufir.models.dtos.UpdateStatusDto;
import com.larrydevincarter.thufir.services.OptionScannerUpdateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public final class OptionScannerClientUtils {

    private static final Logger log = LoggerFactory.getLogger(OptionScannerClientUtils.class);

    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 60_000;

    private OptionScannerClientUtils() {}

    public static void waitForUpdateComplete(OptionScannerUpdateMonitor updateMonitor) {
        if (!updateMonitor.isPotentiallyUpdating()) {
            return;
        }

        int attempts = 0;
        while (attempts < MAX_POLL_ATTEMPTS) {
            attempts++;
            UpdateStatusDto updateStatus = checkUpdateStatus();

            if (updateStatus != null && !updateStatus.isUpdating()) {
                log.info("OptionScanner database update completed after {} attempts — proceeding", attempts);
                updateMonitor.clearUpdateFlag();
                return;
            }

            log.info("OptionScanner still updating (attempt {}/{}) — waiting {} ms",
                    attempts, MAX_POLL_ATTEMPTS, POLL_INTERVAL_MS);

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for OptionScanner update");
                return;
            }
        }

        log.warn("Max poll attempts ({}) reached — OptionScanner still updating. Proceeding with caution.",
                MAX_POLL_ATTEMPTS);
    }

    private static UpdateStatusDto checkUpdateStatus() {
        RestTemplate restTemplate = new RestTemplate();
        String updateStatusUrl = "http://localhost:8081/api/update-status";

        try {
            ResponseEntity<UpdateStatusDto> response = restTemplate.exchange(
                    updateStatusUrl,
                    HttpMethod.GET,
                    null,
                    UpdateStatusDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("Unexpected status from update-status endpoint: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("Failed to check OptionScanner update status: {}", e.getMessage());
            return null;
        }
    }
}