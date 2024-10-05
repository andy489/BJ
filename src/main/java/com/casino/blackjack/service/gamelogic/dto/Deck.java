package com.casino.blackjack.service.gamelogic.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.KING_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SPADES_SUIT;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Deck {
    private List<Card> deck;

    public Deck() {
        deck = new ArrayList<>();

        for (int suit = 0; suit <= SPADES_SUIT; suit++) {
            for (int rank = 0; rank <= KING_RANK; rank++) {
                deck.add(Card.of(suit, rank));
            }
        }
    }
}
