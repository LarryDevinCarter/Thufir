package com.larrydevincarter.thufir.services.impl;

import com.larrydevincarter.thufir.models.entities.ProfitGoal;
import com.larrydevincarter.thufir.repositories.ProfitGoalRepository;
import com.larrydevincarter.thufir.services.ProfitGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfitGoalServiceImpl implements ProfitGoalService {

    private final ProfitGoalRepository repo;

    public Optional<ProfitGoal> getCurrent() {
        return repo.findTopByOrderByUpdatedAtDesc();
    }

    public ProfitGoal setNewProfitGoal(BigDecimal newAmount) {
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Profit goal must be positive");
        }

        // Optional: archive old one or just keep history (for now we overwrite by creating new)
        ProfitGoal goal = new ProfitGoal();
        goal.setAmount(newAmount);
        goal.setProfitTaken(false);           // reset flag on new P

        return repo.save(goal);
    }

    public void markProfitTaken() {
        getCurrent().ifPresent(goal -> {
            if (!goal.isProfitTaken()) {
                goal.setProfitTaken(true);
                goal.setProfitTakenAt(LocalDateTime.now());
                repo.save(goal);
            }
        });
    }

    public boolean isProfitTakingAllowed() {
        return getCurrent()
                .map(g -> !g.isProfitTaken())
                .orElse(false);   // no P set → not allowed
    }

    public Optional<BigDecimal> getCurrentAmount() {
        return getCurrent().map(ProfitGoal::getAmount);
    }
}