package com.casino.blackjack.model.dto;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BetHistoryView {

    private static final Map<Integer, String> SUIT_SYMBOL = Map.of(
            0, "♣", 1, "♦", 2, "♥", 3, "♠"
    );
    private static final Map<Integer, String> RANK_LABEL = Map.ofEntries(
            Map.entry(1, "A"), Map.entry(2, "2"), Map.entry(3, "3"), Map.entry(4, "4"),
            Map.entry(5, "5"), Map.entry(6, "6"), Map.entry(7, "7"), Map.entry(8, "8"),
            Map.entry(9, "9"), Map.entry(10, "10"), Map.entry(11, "J"), Map.entry(12, "Q"),
            Map.entry(13, "K")
    );

    private static final Map<Integer, String> CHOICE_LABEL = Map.ofEntries(
            Map.entry(1,  "Surrender"),
            Map.entry(2,  "Split"),
            Map.entry(3,  "Double"),
            Map.entry(6,  "Double ✓"),
            Map.entry(7,  "Double ✗"),
            Map.entry(8,  "Stand"),
            Map.entry(9,  "Hit"),
            Map.entry(11, "Even ✓"),
            Map.entry(12, "Even ✗"),
            Map.entry(13, "Insurance ✓"),
            Map.entry(15, "Insurance ✗")
    );

    /** One split hand: its card labels and result multiplier. */
    public static class SplitHandView {
        private final int handNumber;
        private final List<String> cardLabels;
        private final double multiplier;

        SplitHandView(int handNumber, List<String> cardLabels, double multiplier) {
            this.handNumber = handNumber;
            this.cardLabels = cardLabels;
            this.multiplier = multiplier;
        }

        public int getHandNumber()          { return handNumber; }
        public List<String> getCardLabels() { return cardLabels; }
        public double getMultiplier()       { return multiplier; }

        /** > 1.0 = win, == 1.0 = push, < 1.0 = loss (0 = bust/loss) */
        public String getResultClass() {
            if (multiplier > 1.0) return "hist-win";
            if (multiplier == 1.0) return "hist-push";
            return "hist-loss";
        }
        public String getResultBadgeClass() {
            if (multiplier > 1.0) return "hist-badge-win";
            if (multiplier == 1.0) return "hist-badge-push";
            return "hist-badge-loss";
        }
        public String getResultLabel() {
            if (multiplier > 1.0) return "win";
            if (multiplier == 1.0) return "push";
            return "loss";
        }
    }

    private final BigDecimal totalBet;
    private final BigDecimal returnAmount;
    private final boolean doubleDown;
    private final boolean split;
    private final boolean insurance;
    private final LocalDateTime finalizedTime;
    private final List<String> playerCardLabels;
    private final List<String> dealerCardLabels;
    private final List<String> actionLabels;
    private final List<SplitHandView> splitHandViews;

    private BetHistoryView(BetHistoryEntity h, ObjectMapper om) {
        this.totalBet      = h.getTotalBetAmount();
        this.returnAmount  = h.getReturnAmount();
        this.doubleDown    = Boolean.TRUE.equals(h.getDoubleDown());
        this.split         = Boolean.TRUE.equals(h.getSplit());
        this.insurance     = Boolean.TRUE.equals(h.getPlayedGame().getInsurance());
        this.finalizedTime = h.getPlayedGame().getFinalizedTime();

        this.playerCardLabels = parseCards(h.getPlayedGame().getPlayerCards(), om);
        this.dealerCardLabels = parseCards(h.getPlayedGame().getDealerCards(), om);
        this.actionLabels     = parseActions(h.getPlayedGame().getTakenChoices(), om);
        this.splitHandViews   = parseSplitHands(
                h.getPlayedGame().getSplitHands(),
                h.getPlayedGame().getSplitHandMultipliers(),
                om);
    }

    public static BetHistoryView of(BetHistoryEntity h, ObjectMapper om) {
        return new BetHistoryView(h, om);
    }

    private static List<String> parseCards(String json, ObjectMapper om) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Card> cards = om.readValue(json, new TypeReference<>() {});
            return cards.stream()
                    .map(c -> RANK_LABEL.getOrDefault(c.getRank(), "?")
                            + SUIT_SYMBOL.getOrDefault(c.getSuit(), "?"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static List<String> parseActions(String json, ObjectMapper om) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Integer> choices = om.readValue(json, new TypeReference<>() {});
            return choices.stream()
                    .filter(CHOICE_LABEL::containsKey)
                    .map(CHOICE_LABEL::get)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static List<SplitHandView> parseSplitHands(String handsJson, String multipliersJson, ObjectMapper om) {
        if (handsJson == null || handsJson.isBlank()) return Collections.emptyList();
        try {
            List<List<Card>> hands = om.readValue(handsJson, new TypeReference<>() {});
            List<Double> multipliers = (multipliersJson != null && !multipliersJson.isBlank())
                    ? om.readValue(multipliersJson, new TypeReference<>() {})
                    : Collections.emptyList();

            List<SplitHandView> result = new java.util.ArrayList<>();
            for (int i = 0; i < hands.size(); i++) {
                List<String> labels = hands.get(i).stream()
                        .map(c -> RANK_LABEL.getOrDefault(c.getRank(), "?")
                                + SUIT_SYMBOL.getOrDefault(c.getSuit(), "?"))
                        .collect(Collectors.toList());
                double mult = (i < multipliers.size()) ? multipliers.get(i) : 0.0;
                result.add(new SplitHandView(i + 1, labels, mult));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public BigDecimal getTotalBet()              { return totalBet; }
    public BigDecimal getReturnAmount()          { return returnAmount; }
    public boolean isDoubleDown()                { return doubleDown; }
    public boolean isSplit()                     { return split; }
    public boolean isInsurance()                 { return insurance; }
    public LocalDateTime getFinalizedTime()      { return finalizedTime; }
    public List<String> getPlayerCardLabels()    { return playerCardLabels; }
    public List<String> getDealerCardLabels()    { return dealerCardLabels; }
    public List<String> getActionLabels()        { return actionLabels; }
    public List<SplitHandView> getSplitHandViews() { return splitHandViews; }

    /** Net result for the whole round: positive = win, zero = push, negative = loss */
    public int resultSign() {
        return returnAmount.compareTo(totalBet);
    }
}
