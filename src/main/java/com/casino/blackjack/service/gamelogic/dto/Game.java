package com.casino.blackjack.service.gamelogic.dto;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.rng.RNG.randRank;
import static com.casino.blackjack.service.gamelogic.rng.RNG.randRankNotTen;
import static com.casino.blackjack.service.gamelogic.rng.RNG.randRankTen;
import static com.casino.blackjack.service.gamelogic.rng.RNG.randSuit;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CARDS_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_DISPLAY_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DEALER_THRESHOLD_17;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DISPLACEMENT_BASE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DISPLAY_BUST_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERRORS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.FIVE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.FOUR_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.INITIAL_DEALT_CARD_COUNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.INSURANCE_MULTIPLIER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.JAKE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NINE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NINE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_ID_STR;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_TAKEN_CHOICES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ONE_CARD;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SEVEN_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SURRENDER_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.TEN_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.THREE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.TWO_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

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

    private Boolean insurance;
    private Boolean doubleDown;

    private Double handMultiplier;
    private Double insuranceMultiplier;

    private Card dealerSecondCard;

    private LocalDateTime dealtTime;

    private Boolean finalized;

    private Wallet wallet;

    private List<Integer> errCodeList;

    private Map<String, Integer> availableChoicesCodeMap;
    private Map<String, Integer> errCodeMap;

    public Game() {
        hash = NO_ID_STR;
        dealt = false;

        dealerCards = new ArrayList<>();
        playerCards = new ArrayList<>();

        availableChoices = new ArrayList<>();
        takenChoices = new ArrayList<>();

        handMultiplier = 0.0d;
        insuranceMultiplier = 0.0d;

        dealerSecondCard = null;

        insurance = false;
        doubleDown = false;

        errCodeList = new ArrayList<>();

        availableChoicesCodeMap = fillAvailableChoicesMap();
        errCodeMap = fillErrMap();

        finalized = false;
    }

    public Game(Game game) {
        this.hash = game.hash;
        this.dealt = game.dealt;
        this.dealerCards = game.dealerCards;
        this.playerCards = game.playerCards;
        this.availableChoices = game.availableChoices;
        this.takenChoices = game.takenChoices;
        this.handMultiplier = game.handMultiplier;
        this.insuranceMultiplier = game.insuranceMultiplier;
        this.insurance = game.insurance;
        this.doubleDown = game.doubleDown;
        this.dealerSecondCard = game.dealerSecondCard;
        this.finalized = game.finalized;
        this.dealtTime = game.dealtTime;
        this.wallet = game.wallet;
        this.errCodeList = game.errCodeList;
        this.availableChoicesCodeMap = game.availableChoicesCodeMap;
        this.errCodeMap = game.errCodeMap;
    }

    public static Game of(GameEntity gameEntity, ObjectMapper om, WalletEntity walletEntity) {

        Game game = of(gameEntity, om);

        Wallet wallet = new Wallet()
                .setBalance(walletEntity.getBalance())
                .setLastWin(walletEntity.getLastWin())
                .setLastBet(walletEntity.getLastBet())
                .setCurrentBet(walletEntity.getCurrentBet())
                .setHandBet(walletEntity.getHandBet())
                .setInsuranceBet(walletEntity.getInsuranceBet())
                .setDoubleBet(walletEntity.getDoubleBet());

        return game.setWallet(wallet);
    }

    public static Game of(GameEntity gameEntity, ObjectMapper om) {

        List<Card> dealerCards, playerCards;
        List<Integer> availableChoices, takenChoices, errCodeList;
        Card dealerSecondCard;

        try {
            dealerCards = om.readValue(gameEntity.getDealerCards(), new TypeReference<>() {
            });
            playerCards = om.readValue(gameEntity.getPlayerCards(), new TypeReference<>() {
            });
            availableChoices = om.readValue(gameEntity.getAvailableChoices(), new TypeReference<>() {
            });
            takenChoices = om.readValue(gameEntity.getTakenChoices(), new TypeReference<>() {
            });
            errCodeList = om.readValue(gameEntity.getErrCodeList(), new TypeReference<>() {
            });
            dealerSecondCard = om.readValue(gameEntity.getDealerSecondCard(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new Game()
                .setHash(gameEntity.getHash())
                .setDealt(true)
                .setDealerCards(dealerCards)
                .setPlayerCards(playerCards)
                .setAvailableChoices(availableChoices)
                .setTakenChoices(takenChoices)
                .setInsurance(gameEntity.getInsurance())
                .setDoubleDown(gameEntity.getDoubleDown())
                .setDealerSecondCard(dealerSecondCard)
                .setDealtTime(gameEntity.getDealtTime())
                .setHandMultiplier(gameEntity.getHandMultiplier())
                .setInsuranceMultiplier(gameEntity.getInsuranceMultiplier())
                .setErrCodeList(errCodeList)
                .setFinalized(gameEntity.getFinalized());
    }

    public Game deal() {
        dealt = true;
        dealRandom();
        return this;
    }

    private void dealRandom() {
        dealerCards.add(Card.of(randSuit(), randRank()));
        dealerCards.add(Card.of(randSuit(), randRank()));
        playerCards.add(Card.of(randSuit(), randRank()));
        playerCards.add(Card.of(randSuit(), randRank()));

//        dealerCards.add(Card.of(randSuit(), FOUR_RANK));
//        dealerCards.add(Card.of(randSuit(), ACE_RANK));
//        playerCards.add(Card.of(randSuit(), NINE_RANK));
//        playerCards.add(Card.of(randSuit(), JAKE_RANK));
    }

//    private static int a = 0;
    private void dealerHit() {
//        if (a == 0) {
//            dealerCards.add(Card.of(randSuit(), ACE_RANK));
//            a++;
//        } else if (a == 1) {
//            dealerCards.add(Card.of(randSuit(), THREE_RANK));
//            a++;
//        } else
            dealerCards.add(RNG.randCard());

    }

    private void hit(List<Card> cards) {
        cards.add(Card.of(RNG.randSuit(), randRank()));
    }

    private void hitNoTen(List<Card> cards) {
        cards.add(Card.of(RNG.randSuit(), randRankNotTen()));
    }

    private void hitTen(List<Card> cards) {
        cards.add(Card.of(RNG.randSuit(), randRankTen()));
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

    public Integer dealerCardsOdd() {
        return (dealerCards.size() + 1) % 2;
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

    public Game removeLastChoice() {
        takenChoices.remove(takenChoices.size() - 1);
        return this;
    }

    public Integer getLastChoice() {
        if (takenChoices.isEmpty()) {
            return -1;
        }

        return takenChoices.get(takenChoices.size() - 1);
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

        if (right > BJ_CNT && left <= BJ_CNT) {
            right = left;
        }

        return Count.of(left, right);
    }

    public Game calcHand() {

        if (!dealt || finalized) {
            if (getLastChoice().equals(CHOICE_DOUBLE_DOWN)) {
                doubleDown = true;
                playerDouble();

                if (playerBust()) {
                    dealerPlayOneCard();
                } else {
                    dealerPlayUntilSoft17();
                }
            }

            return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        }

        // DOUBLE DOWN CONFIRM
        if (getLastTakenChoice().equals(CHOICE_DOUBLE_DOWN_YES)) {
            finalized = true;
            doubleDown = true;
            playerDouble();

            Boolean bustPlayer = checkBust(playerCards);

            if (bustPlayer) {
                dealerPlayOneCard();
                handMultiplier = ZERO_MULTI;
            } else {
                dealerPlayUntilSoft17();

                Integer win = checkWin(dealerCards, playerCards);

                if (win < 0) {
                    handMultiplier = ZERO_MULTI;
                } else if (win == 0) {
                    handMultiplier = PUSH_MULTI;
                } else {
                    handMultiplier = DOUBLE_MULTI;
                }
            }

            return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        } else if (getLastTakenChoice().equals(CHOICE_DOUBLE_DOWN_NO)) {
            finalized = false;
        }

        // DOUBLE DOWN
        if (getLastTakenChoice().equals(CHOICE_DOUBLE_DOWN)) {
            finalized = false;
            return this;
        }

        // SURRENDER
        if (getLastTakenChoice().equals(CHOICE_SURRENDER)) {
            finalized = true;
            dealerPlayOneCard();
            handMultiplier = SURRENDER_MULTI;
            return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        }

        // PLAYER BJ AFTER DEAL
        if (getLastTakenChoice().equals(CHOICE_DEAL) && checkBJ(playerCards)) {
            if (dealerCannotMakeBJ()) {
                dealerPlayOneCard();
                finalized = true;
                handMultiplier = BJ_MULTI;
                return setAvailableChoices(List.of(CHOICE_DEAL));
            } else {
                return setAvailableChoices(List.of(CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO));
            }
        }

        // YES OR NO EVEN MONEY
        if (getLastTakenChoice() >= CHOICE_EVEN_MONEY_YES && getLastTakenChoice() <= CHOICE_EVEN_MONEY_NO) {

            dealerPlayOneCard();
            finalized = true;

            if (getLastTakenChoice().equals(CHOICE_EVEN_MONEY_YES)) {
                handMultiplier = DOUBLE_MULTI;
            } else {
                handMultiplier = checkBJ(dealerCards) ? ZERO_MULTI : BJ_MULTI;
            }

            return this.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        }

        // HIT
        if (getLastTakenChoice().equals(CHOICE_HIT)) {
            hit(playerCards);
            Count playerCount = getCount(playerCards);

            if (playerCount.getRight().equals(BJ_CNT)) {
                dealerPlayUntilSoft17();
                finalized = true;

                if (checkBJ(dealerCards)) {
                    handMultiplier = ZERO_MULTI;
                } else {
                    Count dealerCount = getCount(dealerCards);
                    if (dealerCount.getRight().equals(BJ_CNT)) {
                        handMultiplier = PUSH_MULTI;
                    } else {
                        handMultiplier = DOUBLE_MULTI;
                    }
                }

                return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            }

            if (playerCount.getLeft() > BJ_CNT) {
                finalized = true;
                dealerPlayOneCard();
                return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            }

            if (playerCount.getLeft() < BJ_CNT) {
                return setAvailableChoices(List.of(CHOICE_STAND, CHOICE_HIT));
            }
        }

        // STAND
        if (getLastTakenChoice().equals(CHOICE_STAND)) {
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
                handMultiplier = DOUBLE_MULTI;
            } else {
                int x = dealerScore.compareTo(playerScore);
                if (x < 0) {
                    handMultiplier = DOUBLE_MULTI;
                } else if (x == 0) {
                    handMultiplier = PUSH_MULTI;
                } else {
                    handMultiplier = ZERO_MULTI;
                }
            }

            return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        }

        // MAKE OR NOT INSURANCE
        if (Objects.equals(getLastTakenChoice(), CHOICE_INSURANCE_YES) ||
                Objects.equals(getLastTakenChoice(), CHOICE_INSURANCE_NO)) {

            if (getLastTakenChoice().equals(CHOICE_INSURANCE_YES)) {
                insurance = true;
            }

            if (checkBJDealerHiddenCard()) {
                finalized = true;
                handMultiplier = ZERO_MULTI;
                dealerCards.add(dealerSecondCard);

                if (insurance) {
                    insuranceMultiplier = INSURANCE_MULTIPLIER;
                }

                return setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            } else {
                setAvailableChoices(List.of(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN));

                if (checkPair(playerCards)) {
                    availableChoices.add(CHOICE_SPLIT);
                }

                return this;
            }
        }

        // if we are here PLAYER does not have BJ
        if (dealerCards.size() == INITIAL_DEALT_CARD_COUNT) {
            if (dealerCards.get(0).getRank().equals(ACE_RANK)) {
                return setAvailableChoices(List.of(CHOICE_INSURANCE_YES, CHOICE_INSURANCE_NO));
            } else {
                availableChoices.add(CHOICE_SURRENDER);
            }
        }

        availableChoices.addAll(List.of(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN));

        if (checkPair(playerCards)) {
            availableChoices.add(CHOICE_SPLIT);
        }

        return this;
    }

    private boolean playerBust() {
        Count count = getCount(playerCards);
        return count.getLeft().equals(count.getRight()) && count.getRight().compareTo(BJ_CNT) > 0;
    }

    // helper game methods
    private void dealerPlayOneCard() {
        if (dealerSecondCard != null) {
            dealerCards.add(dealerSecondCard);
            dealerSecondCard = null;
        } else {
            hit(dealerCards);
        }
    }

    private void playerDouble() {
        hit(playerCards);
    }

    private void dealerPlayUntilSoft17() {

        Count count = getCount(dealerCards);

        while (count.getRight() < DEALER_THRESHOLD_17 || (count.getLeft() < DEALER_THRESHOLD_17 &&
                count.getRight() > BJ_CNT)) {

            if (dealerSecondCard != null) {
                dealerCards.add(dealerSecondCard);
                dealerSecondCard = null;
            } else {
                dealerHit();
            }

            if (count.getLeft() > BJ_CNT) {
                break;
            }

            count = getCount(dealerCards);
        }
    }

    public Game makeChoice(Integer choice) {
        takenChoices.add(choice);
        return this;
    }

    private boolean checkBJ(List<Card> cards) {
        return checkBJInner(cards);
    }

    private boolean checkBJDealerHiddenCard() {
        return dealerCards.get(0).getRank().equals(ACE_RANK) && dealerSecondCard.getRank() >= TEN_RANK;
    }

    private boolean checkBJDealerCardsAfterDeal() {
        List<Card> dealerCardsAfterDeal = new ArrayList<>(dealerCards);
        dealerCardsAfterDeal.add(dealerSecondCard);

        return checkBJ(dealerCardsAfterDeal);
    }

    private boolean checkBJInner(List<Card> cards) {
        Optional<Integer> ace = cards.stream().map(Card::getRank).filter(r -> r.equals(ACE_RANK)).findAny();
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

    public Game addErr(Integer errCode) {
        this.errCodeList.add(errCode);
        return this;
    }

    private Map<String, Integer> fillAvailableChoicesMap() {
        return CHOICES;
    }

    private Map<String, Integer> fillErrMap() {
        return ERRORS;
    }

    public Game addAvailableChoice(Integer availableChoice) {
        availableChoices.add(availableChoice);
        return this;
    }

    public Game removeAvailableChoice(Integer availableChoice) {
        availableChoices.remove(availableChoice);
        return this;
    }

    public Boolean checkTen(Card card) {
        return card.getRank() >= TEN_RANK;
    }

    // Hide second DEALER card
    public Game adjustDealerCardsAfterDeal() {
        Card secondDealerCardMemo = new Card(dealerCards.get(1));
        dealerCards.remove(1);

        this.dealerSecondCard = secondDealerCardMemo;

        return this;
    }

    public void clearErrors() {
        this.errCodeList = new ArrayList<>();
    }

    public Boolean checkBust(List<Card> cards) {
        Count count = getCount(cards);

        return count.getLeft() > BJ_CNT;
    }

    private Integer checkWin(List<Card> left, List<Card> right) {
        Boolean bustLeft = checkBust(left);
        Boolean bustRight = checkBust(right);

        if (bustLeft && bustRight) {
            return 0;
        } else {
            if (bustLeft) {
                return 1;
            }

            if (bustRight) {
                return -1;
            }
        }

        Count leftCount = getCount(left);
        Count rightCount = getCount(right);

        int leftMax = Math.max(leftCount.getLeft(), (leftCount.getRight() > BJ_CNT ? -1 : leftCount.getRight()));
        int rightMax = Math.max(rightCount.getLeft(), (rightCount.getRight() > BJ_CNT ? -1 : rightCount.getRight()));

        if (leftMax == rightMax) {
            return 0;
        } else {
            return leftMax < rightMax ? 1 : -1;
        }
    }
}
