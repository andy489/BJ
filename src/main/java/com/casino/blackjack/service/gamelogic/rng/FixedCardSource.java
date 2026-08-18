package com.casino.blackjack.service.gamelogic.rng;

import com.casino.blackjack.service.gamelogic.dto.Card;

import java.util.List;

/**
 * Deterministic card source for scenario testing. Cards are returned in the
 * order supplied, then the sequence repeats from the beginning indefinitely.
 * Deal order mirrors production: dealer[0], dealer[1], player[0], player[1],
 * then subsequent hits in the order they are requested.
 */
public class FixedCardSource implements CardSource {

    private final List<Card> cards;
    private int index = 0;

    public FixedCardSource(Card... cards) {
        this.cards = List.of(cards);
    }

    @Override
    public Card next() {
        Card card = cards.get(index);
        index = (index + 1) % cards.size();
        return card;
    }
}
