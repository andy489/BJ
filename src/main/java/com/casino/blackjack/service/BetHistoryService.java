package com.casino.blackjack.service;

import com.casino.blackjack.model.dto.BetHistoryView;
import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.repo.BetHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetHistoryService {

    private final BetHistoryRepository betHistoryRepository;
    private final ObjectMapper objectMapper;

    public BetHistoryService(BetHistoryRepository betHistoryRepository, ObjectMapper objectMapper) {
        this.betHistoryRepository = betHistoryRepository;
        this.objectMapper = objectMapper;
    }

    public void save(BetHistoryEntity betHistoryEntity) {
        betHistoryRepository.save(betHistoryEntity);
    }

    public List<BetHistoryView> getLast10(Long userId) {
        return betHistoryRepository.findTop10ByUserIdOrderByIdDesc(userId)
                .stream()
                .map(h -> BetHistoryView.of(h, objectMapper))
                .collect(Collectors.toList());
    }
}
