package com.larrydevincarter.thufir.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommunicationTools {

    private final RestTemplate restTemplate;

    @Value("${discord.webhook.url:}")
    private String discordWebhookUrl;

    @Tool("""
            Send a message to Larry via the Discord channel.

            Use this whenever you want to:
            • Give a status update, portfolio snapshot, or cycle summary
            • Share a buy / sell recommendation with reasoning
            • Report a price fetch issue, calculation question, or edge case
            • Ask for confirmation, clarification, or override approval
            • Deliver any important alert (price spike, profit trigger, etc.)

            Params:
            • content  → the main message text (keep readable on phone: short lines, bullets, bold **tickers**, `numbers`)
            • urgent   → true ONLY for time-sensitive situations (profit trigger hit, major data failure, high-conviction setup needing quick yes/no). False for routine logs/updates.
            • context  → short label (examples: PORTFOLIO, BUY_NVDA, SELL_TSLA, PROFIT_TRIGGER, DATA_ISSUE, QUESTION)
        
            All messages are logged in Discord.
            urgent=true adds @everyone ping + 🚨 emoji prefix.
        """)
    public String sendMessageToLarry(String content, boolean urgent, String context) {
        String messageBlock = String.format(
                "Time: %s CST\nContent: %s\nContext: D%s\nUrgent: %b\n\n",
                LocalDateTime.now(ZoneId.of("America/Chicago")), content, context, urgent
        );

        if (!discordWebhookUrl.isBlank()) {
            sendDiscordWebhook(content, urgent, context);
        }

        log.info("Message sent to Larry: {} (urgent={}, context={})", content, urgent, context);
        return "Message delivered to Larry";
    }

    private void sendDiscordWebhook(String message, boolean urgent, String context) {
        try {
            Map<String, Object> body = new HashMap<>();

            String finalContent = message;

            if (urgent) {
                finalContent = "@everyone " + message;
            }

            if (urgent) {
                finalContent = "🚨 URGENT [" + context + "]: " + finalContent;
            } else {
                finalContent = "ℹ️ [" + context + "]: " + finalContent;
            }

            body.put("content", finalContent);

            body.put("username", urgent ? "Thufir Alert" : "Thufir Log");
            body.put("avatar_url", "https://imgur.com/hOCbbwF.jpeg");

            restTemplate.postForObject(discordWebhookUrl, body, String.class);
            log.debug("Discord webhook sent: {}", finalContent);
        } catch (Exception e) {
            log.error("Discord webhook delivery failed: {}", e.getMessage());
        }
    }
}