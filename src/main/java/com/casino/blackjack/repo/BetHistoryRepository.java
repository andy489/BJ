package com.casino.blackjack.repo;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetHistoryRepository extends JpaRepository<BetHistoryEntity, Long> {

    List<BetHistoryEntity> findTop10ByUserIdOrderByIdDesc(Long userId);
}
