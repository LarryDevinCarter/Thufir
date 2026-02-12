package com.larrydevincarter.thufir.schedulers;

import com.larrydevincarter.thufir.models.entities.ProfitGoal;
import com.larrydevincarter.thufir.services.Assistant;
import com.larrydevincarter.thufir.services.ProfitGoalService;
import com.larrydevincarter.thufir.tools.CommunicationTools;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketUpdateScheduler {

    private final CommunicationTools communicationTools;
    private final ProfitGoalService profitGoalService;
    private final @Qualifier("workingAssistant") Assistant workingAssistant;

    @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "America/Chicago")
    public void preMarketUpdate() {
        checkAndSendUpdate("Pre-Market Update");
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "America/Chicago")
    public void postMarketUpdate() {
        checkAndSendUpdate("Post-Market Update");
    }

    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "America/Chicago")
    public void eveningStatusUpdate() {
        workingAssistant.chat("""
        Current time is around 5 PM CST. Generate and send a concise evening account status update via sendMessageToLarry.
        
        Required elements:
        - Current profit goal (P) (call getCurrentProfitGoal; if NOT_SET, clearly state "Setup required: Profit goal not defined")
            - Format example: **Current Profit Goal** = **$3,000**
        - Available cash / buying power (call getAvailableCash)
        - Position summary **only for categories where I currently hold a position** (quantity > 0)
          - Use getCurrentPositions to get holdings
          - Use getCurrentMarkPrices with relevant symbols to get latest prices and calculate current market values
          - Group by strategy categories from the Investment Plan:
            - NVDA
            - BTC
            - RKLB
            - TSLA
            - ETH
            - DOGE
            - GOOGL/GOOG (combined if both present)
            - NIKL
            - Other stocks basket (combined if any holdings in the list)
          - For each held category show:
            - Cost basis (I) ≈ total average price × quantity
            - Current market value (V)
            - Unrealized gain/loss (V - I)
            - Profit trigger threshold: I + P (or I + 2P for TSLA)
            - Format example: **NVDA**   Cost: $8,420   Value: $11,200   Gain: +$2,780   Trigger at: $20,920
        - If no positions at all → say "No current positions. Ready to begin sequential funding when capital is added."
        - Context label: EVENING_STATUS
        - Urgent: true
        
        Do NOT include categories with zero position.
        Anchor strictly to current data—no assumptions or projections.
    """);
    }

    private void checkAndSendUpdate(String context) {
        List<ProfitGoal> pending = profitGoalService.getPendingTransfers();
        if (!pending.isEmpty()) {
            StringBuilder message = new StringBuilder("**Pending Profit Transfers**\n\n");
            for (ProfitGoal goal : pending) {
                message.append("• Goal ID: ")
                        .append(goal.getId())
                        .append(" | Amount: $")
                        .append(goal.getAmount().toPlainString())
                        .append(" | Taken: ")
                        .append(goal.getProfitTakenAt())
                        .append("\n");
            }
            message.append("\nTransfer complete? Reply to confirm.");
            communicationTools.sendMessageToLarry(message.toString(), true, context + "_PENDING_TRANSFER");
        }
    }
}