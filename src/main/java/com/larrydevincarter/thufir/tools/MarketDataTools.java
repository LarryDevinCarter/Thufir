package com.larrydevincarter.thufir.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataTools {

    private final RestTemplate restTemplate;

    record VixResult(double value, String asOf, String source, String rawSnippet) {
        String toFormattedString() {
            return String.format("Current VIX: %.2f (as of %s, %s)", value, asOf, source);
        }
    }

    @Tool("Fetch the current or most recent CBOE VIX level from reliable public sources. Returns the value and as-of date.")
    public String getCurrentVix() {
        Optional<VixResult> cnbc = tryCnbc();
        Optional<VixResult> investing = tryInvestingDotCom();

        if (cnbc.isPresent() && investing.isPresent()) {
            double diff = Math.abs(cnbc.get().value - investing.get().value);
            if (diff > 0.5) {
                log.warn("VIX source mismatch: CNBC={} vs Investing={} (diff={})", cnbc.get().value, investing.get().value, diff);
                return String.format("WARNING: Sources disagree | CNBC: %.2f (%s) | Investing: %.2f (%s) | Use with caution or manual check.",
                        cnbc.get().value, cnbc.get().asOf, investing.get().value, investing.get().asOf);
            }
            return cnbc.get().toFormattedString();
        }

        if (cnbc.isPresent()) {
            return cnbc.get().toFormattedString() + " (Investing failed)";
        }
        if (investing.isPresent()) {
            return investing.get().toFormattedString() + " (CNBC failed)";
        }

        log.error("Both VIX sources failed completely");
        return "VIX fetch CRITICAL FAILURE — BOTH SOURCES DOWN — HALT TRADING & ALERT LARRY";
    }

    private Optional<VixResult> tryCnbc() {
        try {
            String url = "https://www.cnbc.com/quotes/.VIX";
            String html = restTemplate.getForObject(url, String.class);

            String marker = "Last |";
            int markerIndex = html.indexOf(marker);
            if (markerIndex == -1) {
                log.warn("CNBC marker not found.");
                return Optional.empty();
            }

            String snippet = html.substring(markerIndex, Math.min(markerIndex + 300, html.length()));

            Matcher timeMatcher = Pattern.compile("(\\d{2}/\\d{2}/\\d{2}\\s*(AM|PM)?\\s*EST)").matcher(snippet);
            String timePart = timeMatcher.find() ? timeMatcher.group(1) : "last close (delayed)";

            Matcher valueMatcher = Pattern.compile("(\\d{1,2}\\.\\d{2})").matcher(snippet.substring(timeMatcher.end()));
            if (valueMatcher.find()) {
                String vixValueStr = valueMatcher.group(1);
                double vixValue = Double.parseDouble(vixValueStr);

                Matcher changeMatcher = Pattern.compile("([+-]\\d{1,2}\\.\\d{2})\\s*\\(([+-]\\d{1,2}\\.\\d{2}%)\\)").matcher(snippet.substring(valueMatcher.end()));
                String change = changeMatcher.find() ? changeMatcher.group(0) : "";

                String formattedAsOf = timePart + (change.isEmpty() ? "" : " " + change);
                return Optional.of(new VixResult(vixValue, formattedAsOf, "CNBC delayed", snippet));
            }

            log.warn("CNBC value extraction failed from snippet: {}", snippet);
            return Optional.empty();
        } catch (Exception e) {
            log.error("CNBC fetch error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<VixResult> tryInvestingDotCom() {
        String url = "https://www.investing.com/indices/volatility-s-p-500";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String html = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            String snippet = null;
            String keyMarker = "data-test=\"instrument-price-last\"";
            int idx = html.indexOf(keyMarker);
            if (idx != -1) {
                snippet = html.substring(Math.max(0, idx - 100), Math.min(html.length(), idx + 300));
                Matcher m = Pattern.compile("(\\d{1,2}\\.\\d{2})").matcher(snippet);
                while (m.find()) {
                    String valueStr = m.group(1);
                    double val = Double.parseDouble(valueStr);
                    if (val >= 5.0 && val <= 80.0) {
                        String context = snippet.substring(Math.max(0, m.start() - 80), Math.min(snippet.length(), m.end() + 150));
                        if (context.contains("+") || context.contains("-") || context.contains("Closed") || context.contains("Day's Range")) {
                            String asOf = context.contains("Closed") ? "closed (delayed)" : "intraday (delayed)";
                            return Optional.of(new VixResult(val, asOf, "Investing.com", snippet));
                        }
                    }
                }
            }

            String[] anchors = {"Closed ·", " + ", "Day's Range", "CBOE Volatility Index"};
            for (String anchor : anchors) {
                idx = html.indexOf(anchor);
                if (idx != -1) {
                    snippet = html.substring(Math.max(0, idx - 200), Math.min(html.length(), idx + 200));
                    Matcher m = Pattern.compile("(\\d{1,2}\\.\\d{2})").matcher(snippet);
                    if (m.find()) {
                        String valueStr = m.group(1);
                        double val = Double.parseDouble(valueStr);
                        if (val >= 5.0 && val <= 80.0) {
                            String asOf = anchor.contains("Closed") ? "closed (delayed)" : "intraday (delayed)";
                            return Optional.of(new VixResult(val, asOf, "Investing.com anchored near '" + anchor + "'", snippet));
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