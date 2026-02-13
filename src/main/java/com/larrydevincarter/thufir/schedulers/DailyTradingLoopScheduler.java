package com.larrydevincarter.thufir.schedulers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.models.BuySequenceRecommendation;
import com.larrydevincarter.thufir.services.Assistant;
import com.larrydevincarter.thufir.tools.TastytradeTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTradingLoopScheduler {

    private final @Qualifier("workingAssistant") Assistant workingAssistant;
    private final ObjectMapper objectMapper;
    private final TastytradeTools tastytradeTools;

    /**
     * Runs once per day at 8:35 AM CST (pre-market open window).
     * Calls getNextBuySequenceRecommendations and messages Larry with results.
     */
    @Scheduled(cron = "0 35 8 * * MON-FRI", zone = "America/Chicago")
    public void dailyBuyRecommendationCheck() throws JsonProcessingException {
        log.info("Daily buy sequence check started");

        BuySequenceRecommendation rec = tastytradeTools.getNextBuySequenceRecommendations();

        try {
            String prompt = """
                    You just received the daily buy sequence recommendation object:
                            
                    %s
                            
                    Current time: around 7:30 AM CST (pre-market).
                            
                    Tasks:
                    1. If no recommendations (empty list or error), send a calm status update.
                    2. If there are recommendations, format a clear, scannable Discord message:
                       - Lead with summary (available cash, number of buys, total deploying)
                       - List each buy numbered, with category, symbol, qty, ≈ cost, price used
                    3. Set urgent=true
                    4. Use context label: DAILY_BUY_SEQUENCE
                    5. Call sendMessageToLarry
                            
                    Do not execute anything yet — only recommend.
                    Anchor strictly to the object data.
                    """.formatted(objectMapper.writeValueAsString(rec));

            workingAssistant.chat(prompt);
        } catch (JsonProcessingException e) {
            log.error("Error parsing json while getting daily buy recommendations: {}", e.getMessage());
        }
    }
}