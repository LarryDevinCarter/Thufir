package com.larrydevincarter.thufir.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                Automatically splits long messages (>1900 chars) into numbered parts to stay safely under Discord's 2000-char limit.
                Use this for all status updates, recommendations, alerts, etc.
                Params:
                • content  → the main message text (short lines, bullets, **bold**, `code` encouraged)
                • urgent   → true only for time-sensitive (profit trigger, major issue)
                • context  → short label (BUY_NVDA, PROFIT_TRIGGER, PORTFOLIO_UPDATE, etc.)
            """)
    public String sendMessageToLarry(String content, boolean urgent, String context) {
        final int MAX_SAFE_LENGTH = 1900;

        String[] parts = splitMessage(content, MAX_SAFE_LENGTH);
        String finalContent = "";

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (parts.length > 1) {
                part = String.format("(%d/%d) %s", i + 1, parts.length, part);
            }

            finalContent = part;

            if (urgent) {
                finalContent = "🚨 URGENT [" + context + "]: " + finalContent;
            } else {
                finalContent = "ℹ️ [" + context + "]: " + finalContent;
            }
            sendDiscordWebhook(finalContent, urgent);
        }

        log.info("Sent message to Larry ({} parts, urgent={}, context={})", parts.length, urgent, context);
        return "Message delivered to Larry (" + parts.length + " part" + (parts.length > 1 ? "s" : "") + ")";
    }

    private void sendDiscordWebhook(String message, boolean urgent) {
        try {
            Map<String, Object> body = new HashMap<>();

            String finalContent = message;

            if (urgent) {
                finalContent = "@everyone " + message;
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

        private String[] splitMessage(String content, int maxLength) {
            if (content.length() <= maxLength) {
                return new String[]{content};
            }

            List<String> chunks = new ArrayList<>();
            int start = 0;

            while (start < content.length()) {
                int end = Math.min(start + maxLength, content.length());

                if (end < content.length()) {
                    int lastNewline = content.lastIndexOf('\n', end);
                    int lastSpace = content.lastIndexOf(' ', end);
                    int splitPoint = Math.max(lastNewline, lastSpace);
                    if (splitPoint > start) {
                        end = splitPoint + 1;
                    }
                }

                chunks.add(content.substring(start, end).trim());
                start = end;
            }

            return chunks.toArray(new String[0]);
        }
}