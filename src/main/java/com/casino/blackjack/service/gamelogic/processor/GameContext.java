package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.config.PaytableProperties;
import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.repo.LastGameRepository;
import com.casino.blackjack.repo.PastGameRepository;
import com.casino.blackjack.repo.WalletRepository;
import com.casino.blackjack.service.BasicStrategy;
import com.casino.blackjack.service.BetHistoryService;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.util.LocalDateTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

public record GameContext(
        Game game,
        GameEntity gameEntity,
        WalletEntity walletEntity,
        LastGameRepository lastGameRepo,
        PastGameRepository pastGameRepo,
        WalletRepository walletRepo,
        BetHistoryService betHistoryService,
        BasicStrategy basicStrategy,
        LocalDateTimeProvider clock,
        ObjectMapper om,
        int maxSplits,
        int resultDisplayMs,
        PaytableProperties paytable
) {}
