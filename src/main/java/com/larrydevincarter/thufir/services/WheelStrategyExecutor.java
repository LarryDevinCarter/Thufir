package com.larrydevincarter.thufir.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.clients.MarketStatusClient;
import com.larrydevincarter.thufir.models.dtos.MarketStatusDto;
import com.larrydevincarter.thufir.models.entities.TradeDecision;
import com.larrydevincarter.thufir.repositories.TradeDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

@Service
public class WheelStrategyExecutor {

    private static final Logger log = LoggerFactory.getLogger(WheelStrategyExecutor.class);

    private final Assistant workingAssistant;
    private final MarketStatusClient marketClient;
    private final ObjectMapper objectMapper;
    private final TradeDecisionRepository decisionRepo;

    public WheelStrategyExecutor(@Qualifier("workingAssistant") Assistant workingAssistant, MarketStatusClient marketClient, ObjectMapper objectMapper, TradeDecisionRepository decisionRepository) {
        this.workingAssistant = workingAssistant;
        this.marketClient = marketClient;
        this.objectMapper = objectMapper;
        this.decisionRepo = decisionRepository;
    }

    public void startDailyWheelCycles() {
        ZoneId cst = ZoneId.of("America/Chicago");
        LocalDateTime now = LocalDateTime.now(cst);

        MarketStatusDto status = marketClient.getStatus();

        if (!status.isTradingDay()) {
            log.info("Not a trading day today — Thufir skipping cycles.");
            return;
        }

        LocalTime marketClose = marketClient.parseCloseTime(status.getTodayCloseTime());
        log.info("Trading day detected — Thufir starting cycles. Close time: {}", marketClose);

        while (true) {
            LocalDateTime current = LocalDateTime.now(cst);

            if (current.toLocalTime().isAfter(marketClose) || current.toLocalTime().equals(marketClose)) {
                log.info("Market close reached ({}) — Thufir ending cycles.", marketClose);
                break;
            }

            executeSingleWheelCycle();

            try {
                Thread.sleep(5 * 60 * 1000);  // 5 minutes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Cycle loop interrupted");
                break;
            }
        }
    }

    private void executeSingleWheelCycle() {
        log.info("Thufir executing wheel cycle at {}", LocalDateTime.now(ZoneId.of("America/Chicago")));

        String currentTime = LocalDateTime.now(ZoneId.of("America/Chicago")).toString();

        String prompt = String.format(
                """
                Current date/time: %s
                
                Perform a full wheel strategy cycle:
                1. Assess current account positions, cash, and existing wheel legs.
                2. Scan for new cash-secured put opportunities (use available tools/data).
                3. Evaluate risk/reward with explicit probabilities and expected values.
                4. Decide next action: enter new put, roll, close, or hold.
                
                Prioritize capital preservation. Halt if VIX >30 or drawdown >10%%.
                
                Output JSON only:
                {
                  "action": "hold|sell_put|roll|close",
                  "ticker": "EXAMPLE",
                  "details": { "example_key": "value" },
                  "rationale": "step-by-step reasoning here",
                  "probability_success": 0.85,
                  "expected_return": "5.2%%"
                }
                """, currentTime);

        String decisionJson = workingAssistant.chat(prompt);

        log.info("Thufir raw decision: {}", decisionJson);

        try {
            // Parse JSON (adjust fields to match Thufir's actual output)
            @SuppressWarnings("unchecked")
            Map<String, Object> decisionMap = objectMapper.readValue(decisionJson, Map.class);

            TradeDecision decision = new TradeDecision();
            decision.setTimestamp(LocalDateTime.now(ZoneId.of("America/Chicago")));
            decision.setAction((String) decisionMap.get("action"));
            decision.setTicker((String) decisionMap.getOrDefault("ticker", null));
            decision.setRationale((String) decisionMap.get("rationale"));
            decision.setDetailsJson(objectMapper.writeValueAsString(decisionMap.get("details")));
            decision.setProbabilitySuccess(getDouble(decisionMap, "probability_success"));
            decision.setExpectedReturn((String) decisionMap.get("expected_return"));

            decisionRepo.save(decision);
            log.info("Decision persisted: {}", decision.getAction());

            // Dry-run execution placeholder
            if ("sell_put".equals(decision.getAction())) {
                log.info("DRY-RUN: Would sell put on {} — details: {}", decision.getTicker(), decision.getDetailsJson());
            } else {
                log.info("DRY-RUN: Action is {} — no trade executed.", decision.getAction());
            }

        } catch (Exception e) {
            log.error("Failed to parse or persist decision JSON: {}", decisionJson, e);
        }
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse {} value '{}' as Double", key, value);
                return null;
            }
        }
        log.warn("Unexpected type for {}: {}", key, value.getClass().getSimpleName());
        return null;
    }
}