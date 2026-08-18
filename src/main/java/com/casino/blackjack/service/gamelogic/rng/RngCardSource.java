package com.casino.blackjack.service.gamelogic.rng;

import com.casino.blackjack.service.gamelogic.dto.Card;

public class RngCardSource implements CardSource {
    @Override
    public Card next() {
        return RNG.randCard();
    }
}
