package com.larrydevincarter.thufir.tools;

import com.larrydevincarter.thufir.clients.TastytradeClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TastytradeTools {

    private final TastytradeClient tastytradeClient;

    @Value("${tastytrade.sandbox.account-number}")
    private String accountNumber;

    @Tool("""
        Fetch current account balances and key metrics from Tastytrade.
        Use this before every trade decision to enforce risk controls:
        - Check available cash vs deployed
        - Verify net liquidation value
        - Confirm buying power is sufficient
        Returns a human-readable summary + raw data for precision.
        """)
    public String getAccountBalances() {
        try {
            Map<String, Object> balances = tastytradeClient.getAccountBalances();

            Double cashBalance = getDouble(balances, "cash-balance");
            Double netLiq = getDouble(balances, "net-liquidating-value");
            Double optionBuyingPower = getDouble(balances, "option-buying-power");
            Double stockBuyingPower = getDouble(balances, "stock-buying-power");
            Double maintenanceRequirement = getDouble(balances, "maintenance-requirement");

            String summary = String.format(
                    "Tastytrade Balances (account %s):\n" +
                            "  Cash Balance:          $%.2f\n" +
                            "  Net Liquidating Value: $%.2f\n" +
                            "  Option Buying Power:   $%.2f\n" +
                            "  Stock Buying Power:    $%.2f\n" +
                            "  Maintenance Requirement: $%.2f\n" +
                            "  Deployed %%:            %.1f%%\n",
                    accountNumber,
                    cashBalance, netLiq, optionBuyingPower, stockBuyingPower, maintenanceRequirement, (maintenanceRequirement != null && netLiq != null && netLiq > 0)
                            ? (maintenanceRequirement / netLiq * 100.0) : 0.0
            );

            return summary + "\nRaw data: " + balances;
        } catch (Exception e) {
            log.error("TastytradeTools.getAccountBalances failed", e);
            return "ERROR: Could not fetch balances right now. Trading should be halted until resolved. " +
                    "Details: " + e.getMessage();
        }
    }

    @Tool("""
        Fetch current open positions in the Tastytrade account.
        Critical for:
        - Checking diversification (5–10 underlyings target)
        - Ensuring no single position exceeds 10% of account value (when >$50k)
        - Seeing existing wheel legs (short puts, covered calls)
        Returns count of underlyings + summary stats.
        """)
    public String getPositionsSummary() {
        try {
            Map<String, Object> positionsData = tastytradeClient.getPositions();
            List<Map<String, Object>> items = (List<Map<String, Object>>) positionsData.get("items");

            if (items == null || items.isEmpty()) {
                return "No open positions in account " + accountNumber + ".";
            }

            int positionCount = items.size();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Open Positions (count: %d):\n", positionCount));

            for (Map<String, Object> pos : items) {
                String symbol = (String) pos.get("symbol");
                String underlying = (String) pos.get("underlying-symbol");
                String instrType = (String) pos.get("instrument-type");
                Double quantity = getDouble(pos, "quantity");
                Double avgPrice = getDouble(pos, "average-open-price");
                Double marketValue = getDouble(pos, "market-value");

                sb.append(String.format(
                        "  %s (%s) | Qty: %.0f | Avg: $%.2f | Mkt Val: $%.2f | Type: %s\n",
                        underlying, symbol, quantity, avgPrice, marketValue, instrType
                ));
            }

            long uniqueUnderlyings = items.stream()
                    .map(m -> (String) m.get("underlying-symbol"))
                    .distinct().count();

            sb.append(String.format("\nUnique underlyings: %d (target: 5–10)\n", uniqueUnderlyings));

            return sb.toString();
        } catch (Exception e) {
            log.error("TastytradeTools.getPositionsSummary failed", e);
            return "ERROR: Could not fetch positions. " + e.getMessage();
        }
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}