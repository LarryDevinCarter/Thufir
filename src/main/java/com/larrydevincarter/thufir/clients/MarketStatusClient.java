package com.larrydevincarter.thufir.clients;

import com.larrydevincarter.thufir.models.dtos.MarketStatusDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.LocalTime;
import java.util.Map;

@Component
public class MarketStatusClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String endpointUrl = "http://localhost:8081/api/market-status";


    public MarketStatusDto getStatus() {
        try {
            Map<String, Object> response = restTemplate.getForObject(endpointUrl, Map.class);
            MarketStatusDto status = new MarketStatusDto();
            status.setTradingDay(Boolean.TRUE.equals(response.get("isTradingDay")));
            status.setTodayCloseTime((String) response.getOrDefault("todayCloseTime", "15:00"));
            return status;
        } catch (Exception e) {
            MarketStatusDto fallback = new MarketStatusDto();
            fallback.setTradingDay(false);
            fallback.setTodayCloseTime("15:00");
            return fallback;
        }
    }

    public LocalTime parseCloseTime(String timeStr) {
        return LocalTime.parse(timeStr);
    }
}