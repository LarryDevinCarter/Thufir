package com.larrydevincarter.thufir.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.clients.MarketStatusClient;
import com.larrydevincarter.thufir.clients.TastytradeClient;
import com.larrydevincarter.thufir.models.dtos.MarketStatusDto;
import com.larrydevincarter.thufir.models.entities.TradeDecision;
import com.larrydevincarter.thufir.repositories.TradeDecisionRepository;
import com.larrydevincarter.thufir.tools.CommunicationTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WheelStrategyExecutor {

    private static final Logger log = LoggerFactory.getLogger(WheelStrategyExecutor.class);

    private final Assistant workingAssistant;
    private final MarketStatusClient marketClient;
    private final TastytradeClient tastytradeClient;
    private final ObjectMapper objectMapper;
    private final TradeDecisionRepository decisionRepo;
    private final CommunicationTools communicationTools;

    public WheelStrategyExecutor(@Qualifier("workingAssistant") Assistant workingAssistant, MarketStatusClient marketClient, TastytradeClient tastytradeClient, ObjectMapper objectMapper, TradeDecisionRepository decisionRepository, CommunicationTools communicationTools) {
        this.workingAssistant = workingAssistant;
        this.marketClient = marketClient;
        this.tastytradeClient = tastytradeClient;
        this.objectMapper = objectMapper;
        this.decisionRepo = decisionRepository;
        this.communicationTools = communicationTools;
    }

//    @Scheduled(fixedRate = 600000000)
    @Scheduled(cron = "0 25 8 * * MON-FRI", zone = "America/Chicago")
    public void startDailyWheelCycles() {
        ZoneId cst = ZoneId.of("America/Chicago");

        MarketStatusDto status = marketClient.getStatus();

        if (!status.isTradingDay()) {
            log.info("Not a trading day today — Thufir skipping cycles.");
            return;
        }

        LocalTime marketClose = marketClient.parseCloseTime(status.getTodayCloseTime());
        log.info("Trading day detected — Thufir starting cycles. Close time: {}", marketClose);

        while (LocalTime.now(cst).isBefore(marketClose)) {

            executeSingleWheelCycle();

            try {
                Thread.sleep(5 * 60 * 1000);
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

        String prompt = String.format("""
            Current time: %s CST
                                        
            Execute wheel cycle per soul + refinements.
            Handle VIX/tool fails by messaging Larry urgently.
            Follow loosening on holds.
        
            Cycle steps:
            1. Call getCurrentVix() — handle fail as above
            2. Call getAccountBalances() — compute committed_cash_pct = sum(CSP commitments) / cash
            3. Call getPositionsSummary() — exposures, uniques, assigned shares
            4. Apply HARD RULES; message me if needed
            5. If safe: Call getStockCandidatesForPuts() for new puts list + chains via getOptionChain
            6. For assigned: Call getCoveredCallOptions or decide sell
            7. Choose ONE best: contract to sell or share limit order
            8. If none → hold + loosen for next
        
            Output ONLY valid JSON:
            {
              "action": "halt" | "hold" | "sell_put" | "sell_call" | "sell_shares_limit",
              "ticker": null | string,
              "strike": null | number,
              "expiry": null | "YYYY-MM-DD",
              "quantity": null | integer (small: 1 for $2k),
              "premium_target": null | number,
              "limit_price": null | number,
              "delta_approx": null | number,
              "yield_per_day_pct": null | number,
              "rationale": "step-by-step: VIX, committed_pct, uniques, exposures, why this (fundamentals/yield), or hold/loosen",
              "probability_success": null | 0.0–1.0,
              "vix_value": number | null,
              "vix_source": string | null
            }
        """, currentTime);

        String decisionJson = workingAssistant.chat(prompt);

        log.info("Thufir raw decision: {}", decisionJson);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> decisionMap = objectMapper.readValue(decisionJson, Map.class);

            TradeDecision decision = new TradeDecision();
            decision.setTimestamp(LocalDateTime.now(ZoneId.of("America/Chicago")));
            decision.setAction((String) decisionMap.get("action"));
            decision.setTicker((String) decisionMap.getOrDefault("ticker", null));
            decision.setRationale((String) decisionMap.get("rationale"));
            decision.setDetailsJson(objectMapper.writeValueAsString(decisionMap));
            decision.setProbabilitySuccess(getDouble(decisionMap, "probability_success"));
            decision.setExpectedReturn(decisionMap.get("expected_monthly_return_pct") + "%");

            decisionRepo.save(decision);
            log.info("Decision persisted: action={}, ticker={}", decision.getAction(), decision.getTicker());

            String action = decision.getAction();

            if ("halt".equals(action) || "hold".equals(action)) {
                log.info("Thufir cycle result: {}", action.toUpperCase());
                communicationTools.sendMessageToLarry(
                        "Cycle result: " + action.toUpperCase() + "\n" + decision.getRationale(),
                        "halt".equals(action),
                        "CYCLE_" + action.toUpperCase()
                );
                return;
            }

            if (!("sell_put".equals(action) || "sell_call".equals(action))) {
                log.warn("Unknown action received: {}", action);
                return;
            }

            Map<String, Object> details = (Map<String, Object>) decisionMap.getOrDefault("details", decisionMap);

            String expiryYYMMDD = ((String) decisionMap.get("expiry")).replace("-", "").substring(2); // e.g. 250321
            String optionType = "sell_put".equals(action) ? "P" : "C";
            String strikeStr = String.format("%.0f", ((Number) decisionMap.get("strike")).doubleValue());
            String symbol = decision.getTicker() + " " + expiryYYMMDD + optionType + strikeStr;

            Map<String, Object> leg = new HashMap<>();
            leg.put("instrument-type", "Equity Option");
            leg.put("symbol", symbol);
            leg.put("quantity", decisionMap.get("quantity"));
            leg.put("action", "Sell to Open");
            leg.put("effect", "Open");

            Map<String, Object> order = new HashMap<>();
            order.put("time-in-force", "Day");
            order.put("order-type", decisionMap.getOrDefault("order_type", "Limit"));
            order.put("price", decisionMap.get("limit_price"));
            order.put("price-effect", "Credit");
            order.put("legs", List.of(leg));

            Map<String, Object> balances = tastytradeClient.getAccountBalances();
            Double cash = getDouble(balances, "cash-balance");
            Double netLiq = getDouble(balances, "net-liquidating-value");
            Double approxRisk = ((Number) decisionMap.get("strike")).doubleValue() * 100 * ((Number) decisionMap.get("quantity")).intValue();

            if (cash == null || netLiq == null || approxRisk > cash * 1.1) {
                log.warn("Pre-execution risk check failed - insufficient cash or excessive risk");
                communicationTools.sendMessageToLarry(
                        "PRE-EXECUTION HALT: Insufficient cash or risk too high for " + decision.getTicker() +
                                "\nRationale: " + decision.getRationale(),
                        true,
                        "TRADE_HALT_RISK"
                );
                return;
            }

            Map<String, Object> orderResult = tastytradeClient.placeOrder(order);

            log.info("Sandbox order placed successfully: {}", orderResult);

            communicationTools.sendMessageToLarry(
                    "SANDBOX TRADE EXECUTED\n" +
                            "Action: " + action + "\n" +
                            "Ticker: " + decision.getTicker() + "\n" +
                            "Strike/Expiry: " + decisionMap.get("strike") + " / " + decisionMap.get("expiry") + "\n" +
                            "Quantity: " + decisionMap.get("quantity") + "\n" +
                            "Rationale excerpt: " + decision.getRationale().substring(0, Math.min(200, decision.getRationale().length())),
                    false,
                    "TRADE_EXEC_SANDBOX"
            );

        } catch (Exception e) {
            log.error("Failed to parse decision or execute: {}", decisionJson, e);
            communicationTools.sendMessageToLarry(
                    "CRITICAL: Wheel cycle decision parsing / execution failed\nRaw JSON:\n" + decisionJson + "\nError: " + e.getMessage(),
                    true,
                    "CYCLE_CRITICAL_ERROR"
            );
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