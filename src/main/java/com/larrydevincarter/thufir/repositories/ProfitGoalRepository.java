package com.larrydevincarter.thufir.repositories;

import com.larrydevincarter.thufir.models.entities.ProfitGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfitGoalRepository extends JpaRepository<ProfitGoal, Long> {

    Optional<ProfitGoal> findTopByProfitTakenFalseOrderByCreatedAtDesc();

    long count();

    List<ProfitGoal> findByProfitTakenTrueAndFundsTransferredFalseOrderByProfitTakenAtDesc();
}
