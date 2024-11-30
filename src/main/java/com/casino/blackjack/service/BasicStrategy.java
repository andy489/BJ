package com.casino.blackjack.service;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.EIGHT_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ELEVEN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.FIVE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.FOUR_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.INITIAL_DEALT_CARD_COUNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NINE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NINE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SEVEN_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIX_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.TEN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.TEN_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.THREE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.TWO_RANK;

@Component
public class BasicStrategy {

    public Boolean getDoubleDown(Game game) {

        List<Card> playerCards = game.getPlayerCards();
        List<Card> dealerCards = game.getDealerCards();

        if (playerCards.size() != INITIAL_DEALT_CARD_COUNT) {
            throw new RuntimeException("Expected initial dealt cards, but found " + playerCards.size() + " cards");
        }

        Count playerCount = game.getCount(playerCards);
        Card dealerFirstCard = dealerCards.get(0);

        if (containsCard(playerCards, ACE_RANK)) {
            if (containsCard(playerCards, List.of(TWO_RANK, THREE_RANK))) {
                return List.of(FIVE_RANK, SIX_RANK).contains(dealerFirstCard.getRank());
            } else if (containsCard(playerCards, List.of(FOUR_RANK, FIVE_RANK))) {
                return List.of(FOUR_RANK, FIVE_RANK, SIX_RANK).contains(dealerFirstCard.getRank());
            } else if (containsCard(playerCards, List.of(SIX_RANK, SEVEN_RANK))) {
                return List.of(THREE_RANK, FOUR_RANK, FIVE_RANK, SIX_RANK).contains(dealerFirstCard.getRank());
            }
        } else {
            if (playerCount.getRight().equals(NINE)) {
                return List.of(THREE_RANK, FOUR_RANK, FIVE_RANK, SIX_RANK).contains(dealerFirstCard.getRank());
            } else if (playerCount.getRight().equals(TEN)) {
                return List.of(TWO_RANK, THREE_RANK, FOUR_RANK, FIVE_RANK, SIX_RANK, SEVEN_RANK, EIGHT_RANK, NINE_RANK)
                        .contains(dealerFirstCard.getRank());
            } else if (playerCount.getRight().equals(ELEVEN)) {
                return List.of(TWO_RANK, THREE_RANK, FOUR_RANK, FIVE_RANK, SIX_RANK, SEVEN_RANK, EIGHT_RANK, NINE_RANK,
                                TEN_RANK)
                        .contains(dealerFirstCard.getRank());
            }
        }

        return false;
    }

    private Boolean containsCard(List<Card> cards, Integer rank) {
        return cards.stream().anyMatch(c -> Objects.equals(c.getRank(), rank));
    }

    private Boolean containsCard(List<Card> cards, List<Integer> ranks) {
        return cards.stream().anyMatch(c -> ranks.contains(c.getRank()));
    }
}
