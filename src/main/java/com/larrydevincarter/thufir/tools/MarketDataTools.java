package com.larrydevincarter.thufir.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class MarketDataTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool("Fetch the current or most recent CBOE VIX level from reliable public sources. Returns the value and as-of date.")
    public String getCurrentVix() {
        try {
            String url = "https://www.cnbc.com/quotes/.VIX";
            String html = restTemplate.getForObject(url, String.class);

            String marker = "Last |";
            int markerIndex = html.indexOf(marker);
            if (markerIndex == -1) {
                return "Unable to find 'Last |' marker on CNBC page.";
            }

            String snippet = html.substring(markerIndex, Math.min(markerIndex + 150, html.length()));

            String[] parts = snippet.split("\\s+");
            String vixValue = null;
            String timePart = null;

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].matches("\\d{1,2}:\\d{2}\\s*(AM|PM)\\s*EST")) {
                    timePart = parts[i] + " " + parts[i+1];
                }
                if (vixValue == null && parts[i].matches("\\d{1,2}\\.\\d{2}")) {
                    vixValue = parts[i];
                    break;
                }
            }

            if (vixValue != null) {
                String asOf = (timePart != null) ? timePart : "last close (delayed)";
                return String.format("Current VIX: %s (as of %s, delayed at least 15 min). Source: CNBC.", vixValue, asOf);
            }

            return "Found marker but could not extract numeric VIX value from snippet: " + snippet;
        } catch (Exception e) {
            return "Error fetching or parsing VIX from CNBC: " + e.getMessage();
        }
    }
}