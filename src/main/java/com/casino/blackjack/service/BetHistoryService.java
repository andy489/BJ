package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.repo.BetHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class BetHistoryService {

    private final BetHistoryRepository betHistoryRepository;


    public BetHistoryService(BetHistoryRepository betHistoryRepository) {
        this.betHistoryRepository = betHistoryRepository;
    }

    public void save(BetHistoryEntity betHistoryEntity) {
        betHistoryRepository.save(betHistoryEntity);
    }
}
