package com.casino.blackjack.service.gamelogic.dto;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.rng.RNG.randRank;
import static com.casino.blackjack.service.gamelogic.rng.RNG.randSuit;
import static com.casino.blackjack.service.gamelogic.util.Util.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_CARDS_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_DISPLAY_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.BJ_MULTI;
import static com.casino.blackjack.service.gamelogic.util.Util.CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.Util.CLUBS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.Util.DEAL;
import static com.casino.blackjack.service.gamelogic.util.Util.DEALER_THRESHOLD_17;
import static com.casino.blackjack.service.gamelogic.util.Util.DIAMONDS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.Util.DISPLACEMENT_BASE;
import static com.casino.blackjack.service.gamelogic.util.Util.DISPLAY_BUST_CNT;
import static com.casino.blackjack.service.gamelogic.util.Util.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.Util.EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.Util.EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.Util.HEARTS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.Util.HIT;
import static com.casino.blackjack.service.gamelogic.util.Util.INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.Util.INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.Util.JAKE_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.KING_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.NINE_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.NO_ID_STR;
import static com.casino.blackjack.service.gamelogic.util.Util.NO_TAKEN_CHOICES;
import static com.casino.blackjack.service.gamelogic.util.Util.ONE_CARD;
import static com.casino.blackjack.service.gamelogic.util.Util.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.Util.SPADES_SUIT;
import static com.casino.blackjack.service.gamelogic.util.Util.STAND;
import static com.casino.blackjack.service.gamelogic.util.Util.SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.Util.SURRENDER_MULTI;
import static com.casino.blackjack.service.gamelogic.util.Util.TEN_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.THREE_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.TWO_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.ZERO_MULTI;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Game {

    private String hash;

    private Boolean dealt;

    private List<Card> dealerCards;
    private List<Card> playerCards;

    private List<Integer> availableChoices;
    private List<Integer> takenChoices;

    private Double winMultiplier;

    private Boolean insurance;

    private Boolean secondDealerCardTen;

    private Boolean finalized;

    private Wallet wallet;

    private String errorMessage;

    public Game() {
        hash = NO_ID_STR;
        dealt = false;

        dealerCards = new ArrayList<>();
        playerCards = new ArrayList<>();

        availableChoices = new ArrayList<>();
        takenChoices = new ArrayList<>();

        winMultiplier = 0.0d;
        finalized = false;
    }

    public static Game of(GameEntity gameEntity, ObjectMapper om, WalletEntity walletEntity) {

        List<Card> dealerCards, playerCards;
        List<Integer> availableChoices, takenChoices;

        try {
            dealerCards = om.readValue(gameEntity.getDealerCards(), new TypeReference<>() {
            });
            playerCards = om.readValue(gameEntity.getPlayerCards(), new TypeReference<>() {
            });
            availableChoices = om.readValue(gameEntity.getAvailableChoices(), new TypeReference<>() {
            });
            takenChoices = om.readValue(gameEntity.getTakenChoices(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Game game = new Game()
                .setHash(gameEntity.getHash())
                .setDealt(true)
                .setDealerCards(dealerCards)
                .setPlayerCards(playerCards)
                .setAvailableChoices(availableChoices)
                .setTakenChoices(takenChoices)
                .setInsurance(gameEntity.getInsurance())
                .setSecondDealerCardTen(gameEntity.getSecondDealerCardTen())
                .setFinalized(gameEntity.getFinalized());

        Wallet wallet = new Wallet()
                .setBalance(walletEntity.getBalance())
                .setLastWin(walletEntity.getLastWin())
                .setCurrentBet(walletEntity.getCurrentBet());

        return game.setWallet(wallet);
    }

    public Game deal() {
        dealt = true;
//        dealBJForPlayerAndNoChanceForBJForDealer();
//        dealBJForPlayerAndPotentialBJForBJForDealer();
        dealRandom();

//        dealForSurrender();

        return this;
    }

    private void dealForSurrender() {
        dealerCards.add(Card.of(SPADES_SUIT, TEN_RANK));

        playerCards.add(Card.of(HEARTS_SUIT, THREE_RANK));
        playerCards.add(Card.of(DIAMONDS_SUIT, TWO_RANK));
    }

    private void dealBJForPlayerAndPotentialBJForBJForDealer() {
        dealerCards.add(Card.of(CLUBS_SUIT, ACE_RANK));

        playerCards.add(Card.of(HEARTS_SUIT, ACE_RANK));
        playerCards.add(Card.of(DIAMONDS_SUIT, KING_RANK));
    }

    private void dealBJForPlayerAndNoChanceForBJForDealer() {
        dealerCards.add(Card.of(SPADES_SUIT, THREE_RANK));

        playerCards.add(Card.of(HEARTS_SUIT, ACE_RANK));
        playerCards.add(Card.of(DIAMONDS_SUIT, KING_RANK));
    }

    private void dealRandom() {
        dealerCards.add(Card.of(randSuit(), randRank()));

        playerCards.add(Card.of(randSuit(), randRank()));
        playerCards.add(Card.of(randSuit(), randRank()));
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

    public String dealerScore() {
        String score = getScore(dealerCards);

        int i = score.indexOf('/');

        if (i < 0) {
            return score;
        }

        String substring = score.substring(i + 1);
        int right;
        try {
            right = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }

        if (right >= DEALER_THRESHOLD_17) {
            return score.substring(i + 1);
        } else {
            return score;
        }
    }

    public String playerScore() {
        return getScore(playerCards);
    }

    public String getScore(List<Card> cards) {
        if (checkBJ(cards)) {
            return BJ_DISPLAY_CNT;
        }

        Count count = getCount(cards);

        Integer left = count.getLeft();
        Integer right = count.getRight();

        if (count.getRight().equals(BJ_CNT)) {
            return BJ_CNT + "";
        }

        if (left > BJ_CNT) {
            return DISPLAY_BUST_CNT + " " + left;
        }

        if (!Objects.equals(left, right)) {
            if (right <= BJ_CNT) {
                return left + "/" + right;
            } else {
                return left + "";
            }
        }

        return left + "";
    }

    public Count getCount(List<Card> cards) {

        int left = 0; // generous (count aces as 1)
        int right = 0; // greedy (count aces as 11 if less than 21)

        for (Card currCard : cards) {
            if (currCard.getRank().equals(ACE_RANK)) {
                left += ACE_RANK;

                if (right + ACE_RANK + TEN_RANK <= BJ_CNT) {
                    right += ACE_RANK + TEN_RANK;
                } else {
                    right += ACE_RANK;
                }
            } else {
                if (currCard.getRank() > NINE_RANK) {
                    left += TEN_RANK;
                    right += TEN_RANK;
                } else {
                    left += currCard.getRank();
                    right += currCard.getRank();
                }
            }
        }

        return Count.of(left, right);
    }

    public Game calcHand() {

        if (!dealt || finalized) {
            return setAvailableChoices(List.of(CHIP_OPERATIONS, DEAL));
        }

        // SURRENDER
        if (getLastTakenChoice().equals(SURRENDER)) {
            finalized = true;
            // dealerPlayUntilSoft17();
            dealerPlayOneCard();
            winMultiplier = SURRENDER_MULTI;
            return setAvailableChoices(List.of(DEAL));
        }

        // PLAYER BJ AFTER DEAL
        if (getLastTakenChoice().equals(DEAL) && checkBJ(playerCards)) {
            if (dealerCannotMakeBJ()) {
                finalized = true;
                winMultiplier = BJ_MULTI;
                return setAvailableChoices(List.of(DEAL));
            } else {
                return setAvailableChoices(List.of(EVEN_MONEY_NO, EVEN_MONEY_YES));
            }
        }

        // YES OR NO EVEN MONEY
        if (getLastTakenChoice() >= EVEN_MONEY_YES &&
                getLastTakenChoice() <= EVEN_MONEY_NO) {

            finalized = true;
            takeOneDealerCardToCheckForBJ();

            if (Objects.equals(getLastTakenChoice(), EVEN_MONEY_YES)) {
                winMultiplier = DOUBLE_MULTI;
            } else {
                winMultiplier = checkBJ(dealerCards) ? ZERO_MULTI : BJ_MULTI;
            }

            return setAvailableChoices(List.of(DEAL));
        }

        // HIT
        if (getLastTakenChoice().equals(HIT)) {
            hit(playerCards);
            Count playerCount = getCount(playerCards);

            if (playerCount.getRight().equals(BJ_CNT)) {
                finalized = true;
                dealerPlayUntilSoft17();

                if (checkBJ(dealerCards)) {
                    winMultiplier = ZERO_MULTI;
                } else {
                    Count dealerCount = getCount(dealerCards);
                    if (dealerCount.getRight().equals(BJ_CNT)) {
                        winMultiplier = PUSH_MULTI;
                    } else {
                        winMultiplier = DOUBLE_MULTI;
                    }
                }

                return setAvailableChoices(List.of(DEAL));
            }

            if (playerCount.getLeft() > BJ_CNT) {
                finalized = true;
                // dealerPlayUntilSoft17();
                dealerPlayOneCard();
                return setAvailableChoices(List.of(DEAL));
            }

            if (playerCount.getLeft() < BJ_CNT) {
                return setAvailableChoices(List.of(STAND, HIT));
            }
        }

        // STAND
        if (getLastTakenChoice().equals(STAND)) {
            finalized = true;
            dealerPlayUntilSoft17();

            Count dealerCount = getCount(dealerCards);
            Count playerCount = getCount(playerCards);

            Integer dealerScore = dealerCount.getRight();
            Integer playerScore = playerCount.getRight();
            if (playerScore > BJ_CNT) {
                playerScore = playerCount.getLeft();
            }

            if (dealerScore > BJ_CNT) {
                winMultiplier = DOUBLE_MULTI;
            } else {
                int x = dealerScore.compareTo(playerScore);
                if (x < 0) {
                    winMultiplier = DOUBLE_MULTI;
                } else if (x == 0) {
                    winMultiplier = PUSH_MULTI;
                } else {
                    winMultiplier = ZERO_MULTI;
                }
            }

            return setAvailableChoices(List.of(DEAL));
        }

        // MAKE OR NOT INSURANCE
//        if (getLastTakenChoice() >= INSURANCE_YES &&
//                getLastTakenChoice() <= INSURANCE_NO) {
//
//            if (Objects.equals(getLastTakenChoice(), INSURANCE_YES)) {
//                insurance = ;
//            } else {
//                winMultiplier = checkBJ(dealerCards) ? ZERO_MULTI : BJ_MULTI;
//            }
//
//            return setAvailableChoices(List.of(DEAL));
//        }

        this.availableChoices.addAll(List.of(STAND, HIT));

        if (dealerCards.size() == 1 && !dealerCards.get(0).getRank().equals(ACE_RANK)) {
            this.availableChoices.add(SURRENDER);
        }

        return this;
    }

    // helper game methods
    private void dealerPlayOneCard() {
        hit(dealerCards);
    }

    private void dealerPlayUntilSoft17() {

        while (getCount(dealerCards).getRight() < DEALER_THRESHOLD_17) {
            hit(dealerCards);

            if (getCount(dealerCards).getLeft() > BJ_CNT) {
                break;
            }
        }
    }

    public Game makeChoice(Integer choice) {
        takenChoices.add(choice);
        return this;
    }

    private boolean checkBJ(List<Card> cards) {
        Optional<Integer> ace = cards.stream().map(Card::getRank).filter(r -> r == ACE_RANK).findAny();
        Optional<Integer> ten = cards.stream().map(Card::getRank).filter(r -> r >= TEN_RANK).findAny();

        return cards.size() == BJ_CARDS_CNT && ace.isPresent() && ten.isPresent();
    }

    private boolean checkDealerOnlyOneCardAce() {
        return dealerCards.size() == 1 && dealerCards.get(0).getRank().equals(ACE_RANK);
    }

    private boolean checkPair(List<Card> cards) {
        return cards.size() == 2 &&
                Objects.equals(cards.get(0).getRank(), cards.get(1).getRank());
    }

    private boolean dealerCannotMakeBJ() {
        return dealerCards.size() == ONE_CARD &&
                dealerCards.get(0).getRank() < TEN_RANK &&
                dealerCards.get(0).getRank() > ACE_RANK;
    }

    private Integer getLastTakenChoice() {
        if (takenChoices.isEmpty()) {
            throw new IllegalStateException(NO_TAKEN_CHOICES);
        }

        return takenChoices.get(takenChoices.size() - 1);
    }

    private void takeOneDealerCardToCheckForBJ() {
        dealerCards.add(Card.of(RNG.randSuit(), randRank()));
    }

    private void hit(List<Card> cards) {
        cards.add(Card.of(RNG.randSuit(), randRank()));
    }
}
