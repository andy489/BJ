package com.casino.blackjack.repo;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetHistoryRepository extends JpaRepository<BetHistoryEntity, Long> {
}
