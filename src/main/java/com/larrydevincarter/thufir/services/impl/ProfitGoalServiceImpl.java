package com.larrydevincarter.thufir.services.impl;

import com.larrydevincarter.thufir.models.entities.ProfitGoal;
import com.larrydevincarter.thufir.repositories.ProfitGoalRepository;
import com.larrydevincarter.thufir.services.ProfitGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfitGoalServiceImpl implements ProfitGoalService {

    private final ProfitGoalRepository repo;

    public Optional<ProfitGoal> getCurrent() {
        return repo.findTopByProfitTakenFalseOrderByCreatedAtDesc();
    }

    public ProfitGoal setNewProfitGoal(BigDecimal newAmount) {
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Profit goal must be positive");
        }

        Optional<ProfitGoal> currentOpt = getCurrent();

        if (currentOpt.isPresent()) {
            ProfitGoal current = currentOpt.get();
            if (!current.isProfitTaken()) {
                current.setAmount(newAmount);
                current.setProfitTaken(false);
                current.setUpdatedAt(LocalDateTime.now());
                current.setProfitTakenAt(null);
                return repo.save(current);
            }
        }
        ProfitGoal goal = new ProfitGoal();
        goal.setAmount(newAmount);
        goal.setProfitTaken(false);
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

    public List<ProfitGoal> getPendingTransfers() {
        return repo.findByProfitTakenTrueAndFundsTransferredFalseOrderByProfitTakenAtDesc();
    }

    public ProfitGoal markFundsTransferred() {
        return getPendingTransfers().stream()
                .findFirst()
                .map(goal -> {
                    goal.setFundsTransferred(true);
                    goal.setFundsTransferredAt(LocalDateTime.now());
                    return repo.save(goal);
                })
                .orElseThrow(() -> new IllegalStateException("No pending profit transfers found"));
    }
}