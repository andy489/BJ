package com.casino.blackjack.service.gamelogic.dto;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.service.gamelogic.rng.CardSource;
import com.casino.blackjack.service.gamelogic.rng.RngCardSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CARDS_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DEALER_THRESHOLD_17;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DISPLACEMENT_BASE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DISPLAY_BUST_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERRORS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.INITIAL_DEALT_CARD_COUNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_DISPLAY_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_ID_STR;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_TAKEN_CHOICES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ONE_CARD;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NINE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.TEN_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Game {

    // Default multipliers used in the self-contained autoPlay/autoFinalize path.
    // Real processor-chain path reads these from PaytableProperties via GameContext.
    private static final double BJ_MULTI           = 2.5;
    private static final double SURRENDER_MULTI    = 0.5;
    private static final double INSURANCE_MULTIPLIER = 3.0;

    @ToString.Exclude
    private transient CardSource cardSource = new RngCardSource();

    /** Transient: carries side bet amount from controller to processor; never persisted. */
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private transient String sideBetAmountStr;

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

    // ── Split state ───────────────────────────────────────────────────────────
    /** Pending/completed split hands (excludes the active hand in playerCards). */
    private List<List<Card>> splitHands;
    /** Result multiplier for each split hand, indexed same as splitHands. */
    private List<Double> splitHandMultipliers;
    /** Double-down flag for each split hand, indexed same as splitHands. */
    private List<Boolean> splitDoubleDownFlags;
    /** Whether a split is currently in progress. */
    private Boolean splitActive;
    /** 0 = playing main hand (playerCards); 1+ = index into splitHands. */
    private Integer activeSplitHandIndex;
    /** How many splits have been performed this round. */
    private Integer splitCount;
    /** True when the original split was on Aces (one card per hand, no further action). */
    private Boolean splitAces;
    /** Actions taken per split hand, indexed same as splitHands. */
    private List<List<Integer>> splitHandTakenChoices;

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

        splitHands = new ArrayList<>();
        splitHandMultipliers = new ArrayList<>();
        splitDoubleDownFlags = new ArrayList<>();
        splitActive = false;
        activeSplitHandIndex = 0;
        splitCount = 0;
        splitAces = false;
        splitHandTakenChoices = new ArrayList<>();
    }

    public Game(Game game) {
        this.cardSource = game.cardSource;
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
        this.splitHands = game.splitHands;
        this.splitHandMultipliers = game.splitHandMultipliers;
        this.splitDoubleDownFlags = game.splitDoubleDownFlags;
        this.splitActive = game.splitActive;
        this.activeSplitHandIndex = game.activeSplitHandIndex;
        this.splitCount = game.splitCount;
        this.splitAces = game.splitAces;
        this.splitHandTakenChoices = game.splitHandTakenChoices;
    }

    public static Game of(GameEntity gameEntity, ObjectMapper om, WalletEntity walletEntity) {

        Game game = of(gameEntity, om);

        Wallet wallet = new Wallet()
                .setBalance(walletEntity.getBalance())
                .setLastWin(walletEntity.getLastWin())
                .setLastBet(walletEntity.getLastBet())
                .setLastTotalBet(walletEntity.getLastTotalBet())
                .setCurrentBet(walletEntity.getCurrentBet())
                .setHandBet(walletEntity.getHandBet())
                .setInsuranceBet(walletEntity.getInsuranceBet())
                .setDoubleBet(walletEntity.getDoubleBet())
                .setSplitBet(walletEntity.getSplitBet())
                .setPerfectPairsBet(walletEntity.getPerfectPairsBet())
                .setTwentyOneThreeBet(walletEntity.getTwentyOneThreeBet())
                .setDealerPerfectPairsBet(walletEntity.getDealerPerfectPairsBet())
                .setPpPreviewWin(walletEntity.getPpPreviewWin() != null ? walletEntity.getPpPreviewWin() : BigDecimal.ZERO)
                .setT3PreviewWin(walletEntity.getT3PreviewWin() != null ? walletEntity.getT3PreviewWin() : BigDecimal.ZERO)
                .setDppPreviewWin(walletEntity.getDppPreviewWin() != null ? walletEntity.getDppPreviewWin() : BigDecimal.ZERO);

        return game.setWallet(wallet);
    }

    public static Game of(GameEntity gameEntity, ObjectMapper om) {

        List<Card> dealerCards, playerCards;
        List<Integer> availableChoices, takenChoices, errCodeList;
        Card dealerSecondCard;
        List<List<Card>> splitHands;
        List<Double> splitHandMultipliers;
        List<Boolean> splitDoubleDownFlags;
        List<List<Integer>> splitHandTakenChoices;

        try {
            dealerCards = gameEntity.getDealerCards() != null
                    ? om.readValue(gameEntity.getDealerCards(), new TypeReference<>() {})
                    : new ArrayList<>();
            playerCards = gameEntity.getPlayerCards() != null
                    ? om.readValue(gameEntity.getPlayerCards(), new TypeReference<>() {})
                    : new ArrayList<>();
            availableChoices = gameEntity.getAvailableChoices() != null
                    ? om.readValue(gameEntity.getAvailableChoices(), new TypeReference<>() {})
                    : new ArrayList<>();
            takenChoices = gameEntity.getTakenChoices() != null
                    ? om.readValue(gameEntity.getTakenChoices(), new TypeReference<>() {})
                    : new ArrayList<>();
            errCodeList = gameEntity.getErrCodeList() != null
                    ? om.readValue(gameEntity.getErrCodeList(), new TypeReference<>() {})
                    : new ArrayList<>();
            dealerSecondCard = gameEntity.getDealerSecondCard() != null
                    ? om.readValue(gameEntity.getDealerSecondCard(), new TypeReference<>() {})
                    : null;
            splitHands = gameEntity.getSplitHands() != null
                    ? om.readValue(gameEntity.getSplitHands(), new TypeReference<>() {})
                    : new ArrayList<>();
            splitHandMultipliers = gameEntity.getSplitHandMultipliers() != null
                    ? om.readValue(gameEntity.getSplitHandMultipliers(), new TypeReference<>() {})
                    : new ArrayList<>();
            splitDoubleDownFlags = gameEntity.getSplitDoubleDownFlags() != null
                    ? om.readValue(gameEntity.getSplitDoubleDownFlags(), new TypeReference<>() {})
                    : new ArrayList<>();
            splitHandTakenChoices = gameEntity.getSplitHandTakenChoices() != null
                    ? om.readValue(gameEntity.getSplitHandTakenChoices(), new TypeReference<>() {})
                    : new ArrayList<>();
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
                .setFinalized(gameEntity.getFinalized())
                .setSplitHands(splitHands)
                .setSplitHandMultipliers(splitHandMultipliers)
                .setSplitDoubleDownFlags(splitDoubleDownFlags)
                .setSplitActive(gameEntity.getSplitActive() != null && gameEntity.getSplitActive())
                .setActiveSplitHandIndex(gameEntity.getActiveSplitHandIndex() != null ? gameEntity.getActiveSplitHandIndex() : 0)
                .setSplitCount(gameEntity.getSplitCount() != null ? gameEntity.getSplitCount() : 0)
                .setSplitAces(gameEntity.getSplitAces() != null && gameEntity.getSplitAces())
                .setSplitHandTakenChoices(splitHandTakenChoices);
    }

    public Game deal() {
        dealt = true;
        dealRandom();
        return this;
    }

    private void dealRandom() {
        dealerCards.add(cardSource.next());
        dealerCards.add(cardSource.next());
        playerCards.add(cardSource.next());
        playerCards.add(cardSource.next());
    }

    private void dealerHit() {
        dealerCards.add(cardSource.next());
    }

    private void hit(List<Card> cards) {
        cards.add(cardSource.next());
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
        int n = doubleDown ? playerCards.size() - 1 : playerCards.size();
        return n % 2;
    }

    public Integer dDealerCards() {
        return DISPLACEMENT_BASE - dealerCards.size() / 2;
    }

    public Integer dPlayerCards() {
        int n = doubleDown ? playerCards.size() - 1 : playerCards.size();
        return DISPLACEMENT_BASE - n / 2;
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

    public Integer playerHardScore() {
        return getCount(playerCards).getLeft();
    }

    public Boolean playerIsSoft() {
        Count c = getCount(playerCards);
        return !c.getLeft().equals(c.getRight()) && c.getRight() <= BJ_CNT;
    }

    public String getScore(List<Card> cards) {
        if (checkBJ(cards)) {
            return BJ_DISPLAY_CNT;
        }

        return scoreWithoutBJ(cards);
    }

    /** Score display for split hands — 2-card 21 shows "21", not "BJ". */
    public String getScoreForSplitHand(List<Card> cards) {
        return scoreWithoutBJ(cards);
    }

    private String scoreWithoutBJ(List<Card> cards) {
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
                playerHit();

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
            playerHit();

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
                    // Dealer cannot have a natural BJ here (already handled above).
                    // Even if dealer reaches 21 via multiple cards, player wins (not a push).
                    handMultiplier = DOUBLE_MULTI;
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
    public void dealerPlayOneCard() {
        if (dealerSecondCard != null) {
            dealerCards.add(dealerSecondCard);
            dealerSecondCard = null;
        } else {
            hit(dealerCards);
        }
    }

    public void playerHit() {
        hit(playerCards);
        if (Boolean.TRUE.equals(splitActive) && activeSplitHandIndex != null
                && activeSplitHandIndex >= 0 && activeSplitHandIndex < splitHands.size()) {
            splitHands.set(activeSplitHandIndex, new ArrayList<>(playerCards));
        }
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

            count = getCount(dealerCards);

            if (count.getLeft() > BJ_CNT) {
                break;
            }
        }
    }

    /** Exposed for use by game-state processors. */
    public void dealerPlayUntilSoft17Public() {
        dealerPlayUntilSoft17();
    }

    public boolean checkBJCards(List<Card> cards) {
        return checkBJ(cards);
    }

    public boolean isDealerHiddenCardBJ() {
        return checkBJDealerHiddenCard();
    }

    public boolean isPair() {
        return checkPair(playerCards);
    }

    /** True if another split is available (balance OK check is done in processor). */
    public boolean canSplitAgain(int maxSplits) {
        return splitCount < maxSplits && checkPair(playerCards);
    }

    /**
     * Initialise a split. splitHands[0] = main hand (after split), splitHands[1] = new hand.
     * playerCards = active hand (copy of splitHands[activeSplitHandIndex]).
     * Caller must have already deducted the bet from the wallet.
     */
    public void initSplit(boolean isAces) {
        splitActive = true;
        splitAces = isAces;
        splitCount++;

        Card movedCard = playerCards.remove(1);

        // Left child keeps original position; right child (new hand) is inserted to its right.
        // Play order: rightmost (Hand 1) first, leftmost (Hand N) last.
        hit(playerCards);
        List<Card> leftHand = new ArrayList<>(playerCards);

        List<Card> rightHand = new ArrayList<>();
        rightHand.add(movedCard);
        hit(rightHand);

        if (splitHands.isEmpty()) {
            // First split: left child at index 0, right child (Hand 1) at index 1.
            splitHands.add(leftHand);
            splitHandMultipliers.add(0.0d);
            splitDoubleDownFlags.add(false);
            splitHandTakenChoices.add(new ArrayList<>());
            splitHands.add(rightHand);
            splitHandMultipliers.add(0.0d);
            splitDoubleDownFlags.add(false);
            splitHandTakenChoices.add(new ArrayList<>());
            // Start with the rightmost hand (Hand 1).
            activeSplitHandIndex = 1;
        } else {
            // Re-split: replace active slot with left child, insert right child after it.
            splitHands.set(activeSplitHandIndex, leftHand);
            splitHands.add(activeSplitHandIndex + 1, rightHand);
            splitHandMultipliers.add(activeSplitHandIndex + 1, 0.0d);
            splitDoubleDownFlags.add(activeSplitHandIndex + 1, false);
            splitHandTakenChoices.add(activeSplitHandIndex + 1, new ArrayList<>());
            // Play the newly inserted right child first.
            activeSplitHandIndex++;
        }

        playerCards = new ArrayList<>(splitHands.get(activeSplitHandIndex));
        doubleDown = false;
    }

    /**
     * Called when the current active hand is done.
     * Saves result for this hand and advances to the next, or returns false when all done.
     */
    public boolean advanceSplitHand(double multiplier, boolean wasDouble) {
        // Save completed hand state
        splitHands.set(activeSplitHandIndex, new ArrayList<>(playerCards));
        splitHandMultipliers.set(activeSplitHandIndex, multiplier);
        splitDoubleDownFlags.set(activeSplitHandIndex, wasDouble);

        // Play order: rightmost (highest index) first, leftmost last — decrement.
        activeSplitHandIndex--;

        if (activeSplitHandIndex >= 0) {
            playerCards = new ArrayList<>(splitHands.get(activeSplitHandIndex));
            doubleDown = false;
            return true;
        }

        // All done — point playerCards at the last played hand (index 0) for display
        playerCards = new ArrayList<>(splitHands.get(0));
        return false;
    }

    public List<Card> getActiveHandCards() {
        return playerCards;
    }

    public String activeHandScore() {
        return getScore(playerCards);
    }

    public Integer activeHandHardScore() {
        return getCount(playerCards).getLeft();
    }

    public Boolean activeHandIsSoft() {
        Count c = getCount(playerCards);
        return !c.getLeft().equals(c.getRight()) && c.getRight() <= BJ_CNT;
    }

    /** Hit the active hand (playerCards). */
    public void splitHandHit() {
        hit(playerCards);
    }

    public boolean dealerFirstCardCannotMakeBJ() {
        return dealerCannotMakeBJ();
    }

    /** Point value of dealer's face-up card (Ace=11, 10/J/Q/K=10, others=face). */
    public int dealerFirstCardNominal() {
        if (dealerCards == null || dealerCards.isEmpty()) return 0;
        int r = dealerCards.get(0).getRank();
        if (r == ACE_RANK) return 11;
        if (r >= TEN_RANK) return 10;
        return r;
    }

    /** Point value of the player's pair card (Ace=11, 10/J/Q/K=10, others=face). */
    public int playerPairNominal() {
        if (playerCards == null || playerCards.isEmpty()) return 0;
        int r = playerCards.get(0).getRank();
        if (r == ACE_RANK) return 11;
        if (r >= TEN_RANK) return 10;
        return r;
    }

    public Integer compareHands(List<Card> dealer, List<Card> player) {
        return checkWin(dealer, player);
    }

    public Integer getLastTakenChoicePublic() {
        return getLastTakenChoice();
    }

    public Game makeChoice(Integer choice) {
        takenChoices.add(choice);
        if (Boolean.TRUE.equals(splitActive)
                && activeSplitHandIndex != null
                && activeSplitHandIndex >= 0
                && activeSplitHandIndex < splitHandTakenChoices.size()) {
            splitHandTakenChoices.get(activeSplitHandIndex).add(choice);
        }
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
        if (cards.size() != 2) return false;
        int r0 = cards.get(0).getRank();
        int r1 = cards.get(1).getRank();
        // Same rank, or both are 10-value cards (10, J, Q, K)
        return r0 == r1 || (r0 >= TEN_RANK && r1 >= TEN_RANK);
    }

    private boolean dealerCannotMakeBJ() {
        return !dealerCards.isEmpty() &&
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
