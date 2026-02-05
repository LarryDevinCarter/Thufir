package com.larrydevincarter.thufir.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.clients.OptionScannerClient;
import com.larrydevincarter.thufir.models.dtos.OptionBatchRequestDto;
import com.larrydevincarter.thufir.models.dtos.StockCandidatesRequestDto;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OptionScannerTools {

    private final OptionScannerClient client;
    private final ObjectMapper objectMapper;

    @Tool("""
    Fetch ranked, filtered stock candidates for new cash-secured puts.
    Thufir passes a structured request with hold streak, max count, liquidity, and excluded tickers.
    Returns top-ranked tickers + metrics.
    """)
    public String getStockCandidatesForPuts(
            int holdStreak,
            int maxCandidates,
            double remainingLiquidity,
            String excludedTickersCsv
    ) {
        try {
            StockCandidatesRequestDto dto = new StockCandidatesRequestDto();
            dto.setHoldStreak(holdStreak);
            dto.setLimit(maxCandidates);
            dto.setRemainingLiquidity(remainingLiquidity);
            if (excludedTickersCsv != null && !excludedTickersCsv.isBlank()) {
                dto.setExcludedTickers(List.of(excludedTickersCsv.split(",")));
            }

            List<String> response = client.getStockCandidates(dto);
            return "Stock candidates response:\n" + objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "ERROR fetching candidates: " + e.getMessage();
        }
    }

    @Tool("""
    Fetch batch option chains using pre-fetched prices.
    Provide JSON list of DTOs: [{"ticker":"AAPL","currentPrice":225.50}, ...]
    Returns asset financials and chains per ticker.
    """)
    public String getBatchOptionChains(String tickerPriceDtosJson) {
        try {
            List<OptionBatchRequestDto> dtos = objectMapper.readValue(tickerPriceDtosJson, new TypeReference<>() {});
            Map<String, Object> response = client.getBatchOptionChains(dtos);
            return "Batch option chains:\n" + objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "ERROR batch chains: " + e.getMessage();
        }
    }

    @Tool("""
    Fetch batch covered call candidates using pre-fetched prices and cost basis.
    Provide JSON list of DTOs: [{"ticker":"AAPL","currentPrice":225.50,"costBasis":210.00}, ...]
    Returns asset data and OTM covered call options.
    """)
    public String getBatchCoveredCallCandidates(String tickerPriceDtosJson) {
        try {
            List<OptionBatchRequestDto> dtos = objectMapper.readValue(tickerPriceDtosJson, new TypeReference<>() {});
            Map<String, Object> response = client.getBatchCoveredCallCandidates(dtos);
            return "Batch covered call candidates:\n" + objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "ERROR batch covered calls: " + e.getMessage();
        }
    }
}