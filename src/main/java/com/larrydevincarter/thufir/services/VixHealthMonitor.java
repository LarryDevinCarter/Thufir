package com.larrydevincarter.thufir.services;

import com.larrydevincarter.thufir.tools.CommunicationTools;
import com.larrydevincarter.thufir.tools.MarketDataTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Slf4j
@RequiredArgsConstructor
public class VixHealthMonitor {

    private final MarketDataTools marketDataTools;
    private final CommunicationTools communicationTools;

    /**
     * Daily post-market VIX source health check (~4:30 PM CST / after NYSE close).
     * Sends urgent Discord alert if sources fail or disagree significantly.
     */
    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "America/Chicago")
    public void dailyVixHealthCheck() {
        log.info("Starting daily VIX health check (post-market) at {}",
                LocalDateTime.now(ZoneId.of("America/Chicago")));

        String vixResult = marketDataTools.getCurrentVix();

        boolean isHealthy = true;
        String alertMessage = null;
        String context = "VIX_HEALTH";

        if (vixResult.contains("CRITICAL FAILURE")) {
            isHealthy = false;
            alertMessage = "VIX HEALTH CRITICAL: Both CNBC and Investing.com failed to fetch data.";
        } else if (vixResult.contains("WARNING: Sources disagree")) {
            isHealthy = false;
            alertMessage = "VIX HEALTH WARNING: CNBC and Investing.com values differ significantly.";
        } else if (vixResult.contains("failed")) {
            isHealthy = false;
            alertMessage = "VIX HEALTH DEGRADED: One source failed — fallback in use.";
        }

        if (!isHealthy) {
            String fullAlert = String.format(
                    "%s\n\nFetched result:\n%s\n\nAction required: Check MarketDataTools scraping logic, page structure changes, or network/firewall issues.",
                    alertMessage, vixResult
            );

            log.error(fullAlert);

            communicationTools.sendMessageToLarry(
                    fullAlert,
                    true,
                    context
            );
        } else {
            String summary = String.format(
                    "Daily VIX check summary (post-market):\n" +
                            "Result: %s\n" +
                            "Status: All sources healthy and consistent\n" +
                            "Timestamp: %s CST",
                    vixResult,
                    LocalDateTime.now(ZoneId.of("America/Chicago"))
            );

            log.info(summary);

            communicationTools.sendMessageToLarry(
                    summary,
                    false,
                    "VIX_DAILY_SUMMARY"
            );
        }
    }
}