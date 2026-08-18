package com.casino.blackjack.service.gamelogic.rng;

import com.casino.blackjack.service.gamelogic.dto.Card;

public interface CardSource {
    Card next();
}
