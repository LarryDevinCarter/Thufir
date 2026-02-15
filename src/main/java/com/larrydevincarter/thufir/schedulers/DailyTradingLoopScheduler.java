package com.larrydevincarter.thufir.schedulers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.models.BuySequenceRecommendation;
import com.larrydevincarter.thufir.models.SingleBuyRecommendation;
import com.larrydevincarter.thufir.services.Assistant;
import com.larrydevincarter.thufir.tools.TastytradeTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTradingLoopScheduler {

    private final @Qualifier("workingAssistant") Assistant workingAssistant;
    private final ObjectMapper objectMapper;
    private final TastytradeTools tastytradeTools;

    /**
     * Runs once per day at 8:35 AM CST (post-market open window).
     * Generates buy sequence, executes buys automatically if recommendations exist,
     * then messages Larry with consolidated results (recommendations + execution outcomes).
     */
    @Scheduled(cron = "0 35 8 * * MON-FRI", zone = "America/Chicago")
    public void dailyBuyRecommendationCheck() {
        log.info("Daily buy sequence check and execution started");

        BuySequenceRecommendation rec;
        try {
            rec = tastytradeTools.getNextBuySequenceRecommendations();
        } catch (Exception e) {
            log.error("Error generating buy recommendations", e);
            sendErrorUpdate("Error generating daily buy recommendations: " + e.getMessage());
            return;
        }

        List<String> executionResults = new ArrayList<>();

        if (rec.isHasRecommendations()) {
            log.info("Executing {} buy recommendations", rec.getRecommendations().size());
            for (SingleBuyRecommendation buy : rec.getRecommendations()) {
                String symbol = buy.getSymbol();
                String quantity = buy.getQuantity();
                boolean isCrypto = buy.isCrypto();
                String result;
                try {
                    result = tastytradeTools.executeBuyOrder(symbol, quantity, isCrypto);
                    log.info("Executed buy for {} (qty: {}): {}", symbol, quantity, result);
                } catch (Exception e) {
                    result = "Error: " + e.getMessage();
                    log.error("Failed to execute buy for {} (qty: {}): {}", symbol, quantity, e.getMessage());
                }
                executionResults.add(String.format("Buy %s (qty: %s, crypto: %s): %s", symbol, quantity, isCrypto, result));
            }
        } else {
            log.info("No buy recommendations to execute");
        }

        // Prepare and send consolidated update
        try {
            String recJson = objectMapper.writeValueAsString(rec);
            String resultsStr = executionResults.isEmpty() ? "No executions performed (no recommendations)." : String.join("\n", executionResults);

            String prompt = """
                    You just received the daily buy sequence recommendation object:
                            
                    %s
                            
                    And the execution results (outcomes for each buy attempt):
                            
                    %s
                            
                    Current time: around 8:35 AM CST (post-market open).
                            
                    Tasks:
                    1. If no recommendations or executions, send a calm status update (e.g., available cash, no actions needed).
                    2. Otherwise, format a clear, scannable Discord message:
                       - Lead with summary (available cash before/after, number of buys recommended/executed, total deployed, overall success rate)
                       - List each recommended buy numbered, with category, symbol, qty, ≈ cost, price used
                       - For each, append execution outcome (success with order ID or error message)
                       - Highlight any failures in bold
                    3. Set urgent=true
                    4. Use context label: DAILY_BUY_EXECUTION
                    5. Call sendMessageToLarry
                            
                    Anchor strictly to the provided data—no assumptions or additional actions.
                    """.formatted(recJson, resultsStr);

            workingAssistant.chat(prompt);
        } catch (JsonProcessingException e) {
            log.error("Error preparing JSON for daily buy update: {}", e.getMessage());
            sendErrorUpdate("Error preparing daily buy update: " + e.getMessage());
        }
    }

    private void sendErrorUpdate(String errorMessage) {
        String errorPrompt = """
                Send a Discord message to Larry with the following error:
                        
                %s
                        
                Set urgent=true
                Use context label: DAILY_BUY_ERROR
                Call sendMessageToLarry
                """.formatted(errorMessage);
        workingAssistant.chat(errorPrompt);
    }
}