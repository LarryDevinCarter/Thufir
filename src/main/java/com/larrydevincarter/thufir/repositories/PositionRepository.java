package com.larrydevincarter.thufir.repositories;

import com.larrydevincarter.thufir.models.entities.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findBySymbolOrderBySnapshotAtDesc(String symbol);
}