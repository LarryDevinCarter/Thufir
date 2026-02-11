package com.larrydevincarter.thufir.tools;

import com.larrydevincarter.thufir.services.TastytradeService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
            return balances.toString();  // or use ObjectMapper for pretty JSON
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}