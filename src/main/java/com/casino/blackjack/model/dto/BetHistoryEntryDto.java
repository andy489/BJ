package com.casino.blackjack.model.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BetHistoryEntryDto {

    public static class SplitHandDto {
        private final int handNumber;
        private final List<String> cardLabels;
        private final double multiplier;
        private final String resultLabel;
        private final List<String> actionLabels;
        private final BigDecimal grossAmount;

        public SplitHandDto(int handNumber, List<String> cardLabels, double multiplier,
                            String resultLabel, List<String> actionLabels, BigDecimal grossAmount) {
            this.handNumber   = handNumber;
            this.cardLabels   = cardLabels;
            this.multiplier   = multiplier;
            this.resultLabel  = resultLabel;
            this.actionLabels = actionLabels;
            this.grossAmount  = grossAmount;
        }

        public int getHandNumber()             { return handNumber; }
        public List<String> getCardLabels()    { return cardLabels; }
        public double getMultiplier()          { return multiplier; }
        public String getResultLabel()         { return resultLabel; }
        public List<String> getActionLabels()  { return actionLabels; }
        public BigDecimal getGrossAmount()     { return grossAmount; }
    }

    private final BigDecimal totalBet;
    private final BigDecimal returnAmount;
    private final boolean doubleDown;
    private final boolean split;
    private final boolean insurance;
    private final String finalizedTime;
    private final List<String> playerCardLabels;
    private final List<String> dealerCardLabels;
    private final List<String> actionLabels;
    private final List<SplitHandDto> splitHandViews;
    private final BigDecimal ppBet;
    private final BigDecimal t3Bet;
    private final BigDecimal dppBet;
    private final BigDecimal ppWin;
    private final BigDecimal t3Win;
    private final BigDecimal dppWin;
    private final List<String> initialPlayerCardLabels;
    private final List<String> initialDealerCardLabels;
    private final int resultSign;

    public BetHistoryEntryDto(BetHistoryView v) {
        this.totalBet     = v.getTotalBet();
        this.returnAmount = v.getReturnAmount();
        this.doubleDown   = v.isDoubleDown();
        this.split        = v.isSplit();
        this.insurance    = v.isInsurance();
        this.finalizedTime = v.getFinalizedTime() != null
                ? v.getFinalizedTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yy"))
                : null;
        this.playerCardLabels = v.getPlayerCardLabels();
        this.dealerCardLabels = v.getDealerCardLabels();
        this.actionLabels     = v.getActionLabels();
        this.splitHandViews   = v.getSplitHandViews().stream()
                .map(s -> new SplitHandDto(s.getHandNumber(), s.getCardLabels(), s.getMultiplier(),
                        s.getResultLabel(), s.getActionLabels(), s.getGrossAmount()))
                .toList();
        this.ppBet  = v.getPpBet();
        this.t3Bet  = v.getT3Bet();
        this.dppBet = v.getDppBet();
        this.ppWin  = v.getPpWin();
        this.t3Win  = v.getT3Win();
        this.dppWin = v.getDppWin();
        this.initialPlayerCardLabels = v.getInitialPlayerCardLabels();
        this.initialDealerCardLabels = v.getInitialDealerCardLabels();
        this.resultSign = v.resultSign();
    }

    public BigDecimal getTotalBet()                   { return totalBet; }
    public BigDecimal getReturnAmount()               { return returnAmount; }
    public boolean isDoubleDown()                     { return doubleDown; }
    public boolean isSplit()                          { return split; }
    public boolean isInsurance()                      { return insurance; }
    public String getFinalizedTime()              { return finalizedTime; }
    public List<String> getPlayerCardLabels()         { return playerCardLabels; }
    public List<String> getDealerCardLabels()         { return dealerCardLabels; }
    public List<String> getActionLabels()             { return actionLabels; }
    public List<SplitHandDto> getSplitHandViews()     { return splitHandViews; }
    public BigDecimal getPpBet()                      { return ppBet; }
    public BigDecimal getT3Bet()                      { return t3Bet; }
    public BigDecimal getDppBet()                     { return dppBet; }
    public BigDecimal getPpWin()                      { return ppWin; }
    public BigDecimal getT3Win()                      { return t3Win; }
    public BigDecimal getDppWin()                     { return dppWin; }
    public List<String> getInitialPlayerCardLabels()  { return initialPlayerCardLabels; }
    public List<String> getInitialDealerCardLabels()  { return initialDealerCardLabels; }
    public int getResultSign()                        { return resultSign; }
}
