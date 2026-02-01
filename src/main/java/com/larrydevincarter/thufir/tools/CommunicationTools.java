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
        Send a message to Larry (the builder/owner).
        
        Use this tool whenever you want to:
        - Log observations, status updates, or cycle summaries for the record
        - Share non-time-sensitive insights or market notes
        - Report minor issues or anomalies worth noting
        - Request clarification, approval, or input on edge cases
        - Raise time-sensitive or critical alerts that need immediate attention
        
        Parameters:
        - content: the message text (keep concise, include relevant numbers/context)
        - urgent: set to true ONLY when the situation is time-sensitive or high-importance 
          and delayed response could affect trading decisions, capital preservation, 
          or system reliability. Examples: complete VIX fetch failure, major source mismatch, 
          unexpected halt triggered, high-conviction trade setup needing confirmation, 
          potential risk threshold breach.
          For routine logs, status reports, or low-priority notes → keep urgent=false (no ping).
        - context: short category label to help triage quickly 
          (examples: "CYCLE_STATUS", "VIX_HEALTH", "TRADE_OBSERVATION", "CLARIFICATION_NEEDED", "ALERT")
        
        All messages (urgent or not) are stored persistently and appear in the Discord channel.
        Only urgent=true triggers a notification/ping.
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