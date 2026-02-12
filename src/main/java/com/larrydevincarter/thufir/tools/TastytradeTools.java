package com.larrydevincarter.thufir.tools;

import com.larrydevincarter.thufir.models.OrderLeg;
import com.larrydevincarter.thufir.models.dtos.OrderRequestDto;
import com.larrydevincarter.thufir.models.entities.Position;
import com.larrydevincarter.thufir.services.TastytradeService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TastytradeTools {

    private final TastytradeService accountService;

    @Tool("Get the current available cash / liquid buying power from tastytrade account in USD")
    public String getAvailableCash() {
        try {
            BigDecimal cash = accountService.getAvailableCash();
            return cash.toPlainString();
        } catch (Exception e) {
            return "Error fetching cash balance: " + e.getMessage();
        }
    }

    @Tool("Get full current account balances snapshot as JSON-like string")
    public String getFullBalances() {
        try {
            Map<String, Object> balances = accountService.getCurrentBalances();
            return balances.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Get current positions as JSON-like string")
    public String getCurrentPositions() {
        try {
            List<Position> positions = accountService.fetchAndSavePositions();
            return positions.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("""
            Preview a buy order (dry-run) without executing it.
            Use this FIRST to check buying power impact and estimated fill.
            Params:
            • symbol: e.g. NVDA, BTC/USD, TSLA
            • quantity: number of shares (whole for equity) or amount (crypto)
            • isCrypto: true for BTC/USD, ETH/USD, DOGE/USD
            Returns: JSON-like string with preview result or error
            """)
    public String previewBuyOrder(String symbol, String quantityStr, boolean isCrypto) {
        try {
            BigDecimal qty = new BigDecimal(quantityStr);
            OrderLeg leg = OrderLeg.builder()
                    .instrumentType(isCrypto ? "Cryptocurrency" : "Equity")
                    .symbol(symbol.toUpperCase())
                    .quantity(qty)
                    .action("Buy to Open")
                    .build();

            OrderRequestDto req = OrderRequestDto.builder()
                    .timeInForce("Day")
                    .orderType("Market")
                    .legs(List.of(leg))
                    .build();

            Map<String, Object> result = accountService.placeOrderDryRun(req);
            return result.toString();
        } catch (Exception e) {
            return "Error previewing buy order: " + e.getMessage();
        }
    }

    @Tool("""
            INTERNAL - Execute a confirmed buy order.
            ONLY call AFTER Larry explicitly confirms (e.g. "yes execute", "place the order").
            Use the exact same params as previewBuyOrder.
            Returns: order ID or confirmation message
            """)
    public String executeBuyOrder(String symbol, String quantityStr, boolean isCrypto) {
        try {
            BigDecimal qty = new BigDecimal(quantityStr);
            OrderLeg leg = OrderLeg.builder()
                    .instrumentType(isCrypto ? "Cryptocurrency" : "Equity")
                    .symbol(symbol.toUpperCase())
                    .quantity(qty)
                    .action("Buy to Open")
                    .build();

            OrderRequestDto req = OrderRequestDto.builder()
                    .timeInForce("Day")
                    .orderType("Market")
                    .legs(List.of(leg))
                    .build();

            Map<String, Object> result = accountService.placeOrder(req);
            return result.toString();
        } catch (Exception e) {
            return "Error placing buy order: " + e.getMessage();
        }
    }

    @Tool("""
            Get current mark prices (mid/last trade) for stocks and/or crypto in one call.

            Params:
            • symbols: comma-separated list of tickers, e.g. "NVDA,TSLA,BTC,ETH,DOGE,RKLB"
              - Stocks: use plain ticker (NVDA, TSLA, GOOGL, etc.)
              - Crypto: use BTC, ETH, DOGE (tool adds /USD internally)

            Returns: JSON-like string e.g. {NVDA=1365.42, BTC=92840.50, ...} or error message
            Use this before calculating remaining-to-target, suggested quantities, profit triggers, or any value-based recommendation.
            """)
    public String getCurrentMarkPrices(String symbols) {
        try {
            if (symbols == null || symbols.trim().isEmpty()) {
                return "{}";
            }

            Set<String> all = Arrays.stream(symbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            Set<String> equities = new HashSet<>();
            Set<String> cryptos = new HashSet<>();

            Set<String> knownCryptos = Set.of("BTC", "ETH", "DOGE");

            for (String s : all) {
                if (knownCryptos.contains(s)) {
                    cryptos.add(s);
                } else {
                    equities.add(s);
                }
            }

            Map<String, BigDecimal> prices = accountService.getCurrentMarkPrices(equities, cryptos);
            return prices.toString();  // clean: {NVDA=1365.42, BTC=92840.50, ...}
        } catch (Exception e) {
            log.warn("Price fetch failed for symbols: {}", symbols, e);
            return "Error fetching prices: " + e.getMessage();
        }
    }
}