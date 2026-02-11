package com.larrydevincarter.thufir.schedulers;

import com.larrydevincarter.thufir.models.entities.ProfitGoal;
import com.larrydevincarter.thufir.services.ProfitGoalService;
import com.larrydevincarter.thufir.tools.CommunicationTools;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketUpdateScheduler {

    private final CommunicationTools communicationTools;
    private final ProfitGoalService profitGoalService;

//    @Scheduled(fixedRate = 600000000)
    @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "America/Chicago")
    public void preMarketUpdate() {
        checkAndSendUpdate("Pre-Market Update");
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "America/Chicago")
    public void postMarketUpdate() {
        checkAndSendUpdate("Post-Market Update");
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