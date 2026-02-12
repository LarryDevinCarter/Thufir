package com.larrydevincarter.thufir.tools;

import com.larrydevincarter.thufir.models.OrderLeg;
import com.larrydevincarter.thufir.models.dtos.OrderRequestDto;
import com.larrydevincarter.thufir.models.entities.Position;
import com.larrydevincarter.thufir.services.TastytradeService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
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
}