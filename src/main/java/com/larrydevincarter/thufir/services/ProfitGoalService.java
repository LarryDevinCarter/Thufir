package com.larrydevincarter.thufir.services;

import com.larrydevincarter.thufir.models.entities.ProfitGoal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProfitGoalService {

    Optional<ProfitGoal> getCurrent();
    ProfitGoal setNewProfitGoal(BigDecimal newAmount);
    void markProfitTaken();
    boolean isProfitTakingAllowed();
    Optional<BigDecimal> getCurrentAmount();
    List<ProfitGoal> getPendingTransfers();
    ProfitGoal markFundsTransferred();
}
