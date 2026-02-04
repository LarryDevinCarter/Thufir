package com.larrydevincarter.thufir.tools;

import com.larrydevincarter.thufir.clients.TastytradeClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
    Fetch current open positions in the Tastytrade sandbox account.
    Critical for:
    - Identifying over-exposed underlyings (>10% net liq exposure — exclude from new trades)
    - Calculating committed cash for open cash-secured puts
    - Detecting assigned shares (for covered calls or sell decisions)
    - Seeing existing wheel legs (short puts, covered calls)
    Returns structured summary with per-underlying exposure, committed amounts, and counts.
    """)
    public String getPositionsSummary() {
        try {
            Map<String, Object> positionsData = tastytradeClient.getPositions();
            List<Map<String, Object>> items = (List<Map<String, Object>>) positionsData.get("items");

            if (items == null || items.isEmpty()) {
                return "No open positions in account " + accountNumber + ".";
            }

            // Fetch net liq once for exposure %
            Map<String, Object> balances = tastytradeClient.getAccountBalances();
            Double netLiq = getDouble(balances, "net-liquidating-value");
            if (netLiq == null || netLiq <= 0) {
                netLiq = 1.0;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Open Positions in account %s (total count: %d):\n\n", accountNumber, items.size()));

            double totalCommittedCsp = 0.0;
            Map<String, Double> underlyingExposure = new HashMap<>();
            Map<String, Integer> underlyingCount = new HashMap<>();
            int assignedSharesCount = 0;

            for (Map<String, Object> pos : items) {
                String underlying = (String) pos.get("underlying-symbol");
                String symbol = (String) pos.get("symbol");
                String instrType = (String) pos.get("instrument-type");
                Double qty = getDouble(pos, "quantity");
                Double avgPrice = getDouble(pos, "average-open-price");
                Double mktValue = getDouble(pos, "market-value");
                String side = (String) pos.get("long-short");

                if (qty == null) qty = 0.0;
                if (mktValue == null) mktValue = 0.0;

                sb.append(String.format(
                        "Underlying: %s | Symbol: %s | Type: %s | Qty: %.0f | Avg Price: $%.2f | Mkt Value: $%.2f\n",
                        underlying, symbol, instrType, qty, avgPrice, mktValue
                ));

                double exposure = Math.abs(mktValue);
                underlyingExposure.merge(underlying, exposure, Double::sum);
                underlyingCount.merge(underlying, 1, Integer::sum);

                if ("Equity Option".equals(instrType) && "Put".equalsIgnoreCase((String) pos.getOrDefault("option-type", ""))
                        && qty < 0) { // short position
                    Double strike = getDouble(pos, "strike-price");
                    if (strike != null) {
                        double committed = strike * 100 * Math.abs(qty);
                        totalCommittedCsp += committed;
                        sb.append(String.format("  → Short Put | Committed cash: $%.2f (strike %.2f)\n", committed, strike));
                    }
                }

                if ("Stock".equals(instrType) && qty > 0) {
                    assignedSharesCount += qty.intValue();
                    sb.append("  → Assigned shares (covered call candidate)\n");
                }
            }

            long uniqueUnderlyings = underlyingExposure.size();
            sb.append("\nSummary:\n");
            sb.append(String.format("  Unique underlyings: %d\n", uniqueUnderlyings));
            sb.append(String.format("  Total assigned shares: %d\n", assignedSharesCount));
            sb.append(String.format("  Total CSP committed cash: $%.2f\n", totalCommittedCsp));

            sb.append("\nExposure per underlying (market value % of net liq):\n");
            Double finalNetLiq = netLiq;
            underlyingExposure.forEach((und, exp) -> {
                double pct = (exp / finalNetLiq) * 100.0;
                sb.append(String.format("  %s: $%.2f (%.2f%% of net liq)\n", und, exp, pct));
            });

            return sb.toString();
        } catch (Exception e) {
            log.error("TastytradeTools.getPositionsSummary failed", e);
            return "ERROR: Could not fetch positions summary. Details: " + e.getMessage();
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