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

//    @Scheduled(fixedRate = 600000000)
    @Scheduled(cron = "0 25 8 * * MON-FRI", zone = "America/Chicago")
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
                
                Before any trading decision, you MUST check current VIX level using available tools (getCurrentVix).
                If VIX > 30, immediately output a halt decision and do not proceed with wheel analysis.
                
                Perform a full wheel strategy cycle:
                1. Fetch current VIX using getCurrentVix tool.
                2. If VIX > 30 or fetch fails → action: "halt_vix"
                3. Assess current account positions, cash, and existing wheel legs.
                4. Scan for new cash-secured put opportunities.
                5. Evaluate risk/reward with explicit probabilities and expected values.
                6. Decide next action: enter new put, roll, close, hold, or halt_vix.
                
                Prioritize capital preservation. Never trade if VIX > 30.
                
                Output JSON only:
                {
                  "action": "hold|sell_put|roll|close|halt_vix",
                  "ticker": "EXAMPLE or null",
                  "details": { "example_key": "value" or {} },
                  "rationale": "step-by-step reasoning here, including VIX value and source",
                  "probability_success": 0.85 or null if halt,
                  "expected_return": "5.2%% or null if halt",
                  "vix_value": 17.44 or null,
                  "vix_source": "CNBC delayed" or null
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