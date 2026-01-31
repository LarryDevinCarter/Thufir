package com.larrydevincarter.thufir.repositories;

import com.larrydevincarter.thufir.models.entities.TradeDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeDecisionRepository extends JpaRepository<TradeDecision, Long> {

    List<TradeDecision> findByTimestampAfterOrderByTimestampDesc(LocalDateTime timestamp);

    List<TradeDecision> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    List<TradeDecision> findByActionOrderByTimestampDesc(String action);

    TradeDecision findTopByOrderByTimestampDesc();
}