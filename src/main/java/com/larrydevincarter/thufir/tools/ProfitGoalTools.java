package com.larrydevincarter.thufir.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larrydevincarter.thufir.models.entities.ProfitGoal;
import com.larrydevincarter.thufir.services.ProfitGoalService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProfitGoalTools {

    private final ProfitGoalService service;
    private final ObjectMapper objectMapper;

    @Tool("""
        Returns the current active profit goal P in USD (plain number, no $ sign).
        Returns 'NOT_SET' if no profit goal has been defined yet.
        """)
    public String getCurrentProfitGoal() {
        return service.getCurrentAmount()
                .map(BigDecimal::toPlainString)
                .orElse("NOT_SET");
    }

    @Tool("""
        Sets a new profit goal P.
        Only call when Larry explicitly gives a new target like:
        "my P is 25000", "set profit goal to 18000", "change P to 20000"
        Resets the profit-taken flag automatically.
        Param amount: positive number without $ or commas
        """)
    public String setProfitGoal(String amountStr) {
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            ProfitGoal saved = service.setNewProfitGoal(amount);
            return "Profit goal updated to $" + saved.getAmount().toPlainString() +
                    ". Profit-taking re-enabled.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("""
        INTERNAL TOOL - Call this ONLY after Larry confirms and you actually execute
        a profit-taking sell that realizes approximately P dollars of profit.
        This sets the flag so the same P won't trigger again until P is changed.
        """)
    public String markProfitTakenForCurrentGoal() {
        service.markProfitTaken();
        return "Profit taken flag set for current P. No more triggers until P is updated.";
    }

    @Tool("Check whether profit-taking is currently allowed (returns true/false)")
    public boolean isProfitTakingAllowed() {
        return service.isProfitTakingAllowed();
    }

    @Tool("""
        Get list of profit goals that are taken but not yet transferred.
        Returns JSON array of objects with id, amount, takenAt.
        """)
    public String getPendingTransferGoals() {
        try {
            List<ProfitGoal> pending = service.getPendingTransfers();
            List<Map<String, Object>> data = pending.stream()
                    .map(g -> Map.<String, Object>of(
                            "id", g.getId(),
                            "amount", g.getAmount().toPlainString(),
                            "takenAt", g.getProfitTakenAt() != null ? g.getProfitTakenAt().toString() : "N/A"
                    ))
                    .toList();
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("""
        Mark the latest pending profit goal as funds transferred.
        Call ONLY when Larry confirms via Discord that the transfer is complete.
        """)
    public String markFundsTransferred() {
        try {
            ProfitGoal updated = service.markFundsTransferred();
            return "Funds marked as transferred for goal ID " + updated.getId() +
                    " ($" + updated.getAmount().toPlainString() + ").";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}