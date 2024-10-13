package com.casino.blackjack.service.gamelogic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@ToString
public class Card {
    private Integer suit;
    private Integer rank;

    public Card (Card card) {
         this.rank = card.getRank();
         this.suit = card.getSuit();
    }
}
