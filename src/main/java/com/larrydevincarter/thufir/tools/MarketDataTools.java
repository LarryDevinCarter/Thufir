package com.larrydevincarter.thufir.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@Slf4j
public class MarketDataTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool("Fetch the current or most recent CBOE VIX level from reliable public sources. Returns the value and as-of date.")
    public String getCurrentVix() {
        Optional<String> cnbc = tryCnbc();
        if (cnbc.isPresent()) {
            log.info("CNBC VIX fetch successful: {}", cnbc.get());
            return cnbc.get();
        }

        Optional<String> investing = tryInvestingDotCom();
        if (investing.isPresent()) {
            log.info("Investing.com VIX fetch successful: {}", investing.get());
            return investing.get();
        }

        log.warn("All VIX sources failed — defaulting to halt or manual check.");
        return "All sources failed";  // Or throw RuntimeException for safety in trading context
    }

    public Optional<String> tryCnbc() {
        try {
            String url = "https://www.cnbc.com/quotes/.VIX";
            String html = restTemplate.getForObject(url, String.class);

            String marker = "Last |";
            int markerIndex = html.indexOf(marker);
            if (markerIndex == -1) {
                log.warn("CNBC marker not found.");
                return Optional.empty();
            }

            // Take a larger snippet to capture value and change
            String snippet = html.substring(markerIndex, Math.min(markerIndex + 300, html.length()));  // Increased to 300 for safety

            // Extract time: pattern like "MM/DD/YY TZ" after marker
            java.util.regex.Matcher timeMatcher = java.util.regex.Pattern.compile("(\\d{2}/\\d{2}/\\d{2}\\s*(AM|PM)?\\s*EST)").matcher(snippet);
            String timePart = timeMatcher.find() ? timeMatcher.group(1) : "last close (delayed)";

            // Extract VIX value: first \d{1,2}\.\d{2} after time, ignoring tags
            java.util.regex.Matcher valueMatcher = java.util.regex.Pattern.compile("(\\d{1,2}\\.\\d{2})").matcher(snippet.substring(timeMatcher.end()));  // Start after time
            if (valueMatcher.find()) {
                String vixValue = valueMatcher.group(1);

                // Optional: Extract change for richer info (e.g., "+0.56 (+3.32%)")
                java.util.regex.Matcher changeMatcher = java.util.regex.Pattern.compile("([+-]\\d{1,2}\\.\\d{2})\\s*\\(([+-]\\d{1,2}\\.\\d{2}%)\\)").matcher(snippet.substring(valueMatcher.end()));
                String change = changeMatcher.find() ? changeMatcher.group(0) : "";

                return Optional.of(String.format("Current VIX: %s%s (as of %s, delayed at least 15 min). Source: CNBC.", vixValue, change.isEmpty() ? "" : " " + change, timePart));
            }

            log.warn("CNBC value extraction failed from snippet: {}", snippet);
            return Optional.empty();
        } catch (Exception e) {
            log.error("CNBC fetch error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> tryInvestingDotCom() {
        String url = "https://www.investing.com/indices/volatility-s-p-500";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String html = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            String snippet = null;
            // Target exact attribute from current HTML
            String keyMarker = "data-test=\"instrument-price-last\"";
            int idx = html.indexOf(keyMarker);
            if (idx != -1) {
                snippet = html.substring(Math.max(0, idx - 100), Math.min(html.length(), idx + 300));
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}\\.\\d{2})").matcher(snippet);
                while (m.find()) {
                    String value = m.group(1);
                    double val = Double.parseDouble(value);
                    if (val >= 5.0 && val <= 80.0) {  // realistic VIX bounds
                        // Check proximity to change symbol or "Closed"
                        String context = snippet.substring(Math.max(0, m.start() - 80), Math.min(snippet.length(), m.end() + 150));
                        if (context.contains("+") || context.contains("-") || context.contains("Closed") || context.contains("Day's Range")) {
                            return Optional.of(String.format("Current VIX: %s (closed 30/01, delayed from Investing.com). Change approx. visible nearby.", value));
                        }
                    }
                }
            }

            // Fallback broader search near "Closed ·" or price context
            String[] anchors = {"Closed ·", " + ", "Day's Range", "CBOE Volatility Index"};
            for (String anchor : anchors) {
                idx = html.indexOf(anchor);
                if (idx != -1) {
                    snippet = html.substring(Math.max(0, idx - 200), Math.min(html.length(), idx + 200));
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}\\.\\d{2})").matcher(snippet);
                    if (m.find()) {
                        String value = m.group(1);
                        double val = Double.parseDouble(value);
                        if (val >= 5.0 && val <= 80.0) {
                            return Optional.of(String.format("Current VIX: %s (delayed from Investing.com, anchored near '%s').", value, anchor));
                        }
                    }
                }
            }

            log.warn("Investing.com: Reliable VIX price not extracted: {}", snippet);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Investing.com error: " + e.getMessage());
            return Optional.empty();
        }
    }
}