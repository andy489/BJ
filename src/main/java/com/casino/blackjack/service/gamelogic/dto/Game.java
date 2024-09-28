package com.casino.blackjack.service.gamelogic.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.rng.RNG.randRank;
import static com.casino.blackjack.service.gamelogic.rng.RNG.randSuit;
import static com.casino.blackjack.service.gamelogic.util.Util.ACE;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_CARDS_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_DISPLAY_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.DEAL;
import static com.casino.blackjack.service.gamelogic.util.Util.DIAMONDS;
import static com.casino.blackjack.service.gamelogic.util.Util.DISPLACEMENT_BASE;
import static com.casino.blackjack.service.gamelogic.util.Util.HEARTS;
import static com.casino.blackjack.service.gamelogic.util.Util.KING;
import static com.casino.blackjack.service.gamelogic.util.Util.ONE_CARD;
import static com.casino.blackjack.service.gamelogic.util.Util.SPADES;
import static com.casino.blackjack.service.gamelogic.util.Util.TEN;

@Getter
@Setter
@Accessors(chain = true)
public class Game {

    private String hash;

    private Boolean dealt;

    private List<Card> dealerCards;
    private List<Card> playerCards;

    private List<Integer> availableDecisions;

    private Double winMultiplier;

    private Boolean finalized;

    public Game() {
        hash = "NO ID";
        dealt = false;

        dealerCards = new ArrayList<>();
        playerCards = new ArrayList<>();

        availableDecisions = new ArrayList<>();

        winMultiplier = 0.0d;
        finalized = false;
    }

    public Game deal() {
        dealt = true;

//        dealRandom();
        dealBJ();

        return this;
    }

    private void dealRandom() {
        dealerCards.add(Card.of(randSuit(), randRank()));

        playerCards.add(Card.of(randSuit(), randRank()));
        playerCards.add(Card.of(randSuit(), randRank()));
    }

    private void dealBJ() {
        dealerCards.add(Card.of(SPADES, 3));

        playerCards.add(Card.of(HEARTS, ACE));
        playerCards.add(Card.of(DIAMONDS, KING));
    }

    public Integer dealerCardsCount() {
        return dealerCards.size();
    }

    public Integer playerCardsCount() {
        return playerCards.size();
    }

    public Integer dealerCardsEven() {
        return dealerCards.size() % 2;
    }

    public Integer playerCardsEven() {
        return playerCards.size() % 2;
    }

    public Integer dDealerCards() {
        return DISPLACEMENT_BASE - dealerCards.size() / 2;
    }

    public Integer dPlayerCards() {
        return DISPLACEMENT_BASE - playerCards.size() / 2;
    }

    public Count getDealerCount() {

        return getCount(dealerCards);
    }

    public Count getPlayerCount() {

        return getCount(playerCards);
    }

    public String dealerScore() {
        return getCnt(false);
    }

    public String playerScore() {
        return getCnt(true);
    }

    private String getCnt(Boolean player) {

        Count score;
        Integer cardsCount;
        if (player) {
            score = getPlayerCount();
            cardsCount = playerCardsCount();
        } else {
            score = getDealerCount();
            cardsCount = dealerCardsCount();
        }

        Integer countValue = score.getCountValue();

        if (score.getCountType()) {
            if (score.getCountValue().equals(BJ_CNT) && cardsCount == BJ_CARDS_CNT) {
                return BJ_DISPLAY_CNT;
            }

            return countValue.toString();
        }

        int secondValue = countValue + TEN;

        return countValue + "/" + secondValue;
    }

    private Count getCount(List<Card> cards) {
        boolean hasAce = false;
        int count = 0;

        for (Card currCard : cards) {
            Integer rank = currCard.getRank();

            count += rank > TEN ? TEN : rank;

            if (currCard.getRank().equals(ACE)) {
                hasAce = true;
            }
        }

        if (hasAce) {
            if (count == TEN + ACE) {
                return Count.of(true, count + TEN);
            } else if (count > TEN + ACE) {
                return Count.of(true, count);
            }

            return Count.of(false, count);
        }

        return Count.of(true, count);
    }

    public Game calcHand() {

        if (!dealt) {
            availableDecisions = List.of(DEAL);
        }

        if (checkPlayerBJ()) {
            if (dealerCannotMakeBJ()) {
                availableDecisions = List.of(DEAL);
                finalized = true;
                winMultiplier = 2.5d;
            }
        }

        return this;
    }

    private boolean checkPlayerBJ() {
        return playerCards.size() == BJ_CARDS_CNT &&
                getPlayerCount().getCountValue().equals(BJ_CNT);
    }

    private boolean dealerCannotMakeBJ() {
        return dealerCards.size() == ONE_CARD &&
                dealerCards.get(0).getRank() < TEN &&
                dealerCards.get(0).getRank() > ACE;
    }
}
