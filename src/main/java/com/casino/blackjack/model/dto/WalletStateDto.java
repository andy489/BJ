package com.casino.blackjack.model.dto;

import java.math.BigDecimal;

public class WalletStateDto {
    private BigDecimal balance;
    private BigDecimal currentBet;
    private BigDecimal lastBet;
    private BigDecimal lastWin;
    private BigDecimal lastHandWin;
    private BigDecimal lastPpWin;
    private BigDecimal lastT3Win;
    private BigDecimal lastDppWin;
    private BigDecimal lastTotalBet;
    private BigDecimal lastPpBet;
    private BigDecimal lastT3Bet;
    private BigDecimal lastDppBet;
    private BigDecimal perfectPairsBet;
    private BigDecimal twentyOneThreeBet;
    private BigDecimal dealerPerfectPairsBet;
    private BigDecimal handBet;
    private BigDecimal doubleBet;
    private BigDecimal insuranceBet;
    private BigDecimal splitBet;

    public WalletStateDto() {}

    public BigDecimal getBalance()                  { return balance; }
    public BigDecimal getCurrentBet()               { return currentBet; }
    public BigDecimal getLastBet()                  { return lastBet; }
    public BigDecimal getLastWin()                  { return lastWin; }
    public BigDecimal getLastHandWin()              { return lastHandWin; }
    public BigDecimal getLastPpWin()                { return lastPpWin; }
    public BigDecimal getLastT3Win()                { return lastT3Win; }
    public BigDecimal getLastDppWin()               { return lastDppWin; }
    public BigDecimal getLastTotalBet()             { return lastTotalBet; }
    public BigDecimal getLastPpBet()                { return lastPpBet; }
    public BigDecimal getLastT3Bet()                { return lastT3Bet; }
    public BigDecimal getLastDppBet()               { return lastDppBet; }
    public BigDecimal getPerfectPairsBet()          { return perfectPairsBet; }
    public BigDecimal getTwentyOneThreeBet()        { return twentyOneThreeBet; }
    public BigDecimal getDealerPerfectPairsBet()    { return dealerPerfectPairsBet; }
    public BigDecimal getHandBet()                  { return handBet; }
    public BigDecimal getDoubleBet()                { return doubleBet; }
    public BigDecimal getInsuranceBet()             { return insuranceBet; }
    public BigDecimal getSplitBet()                 { return splitBet; }

    public WalletStateDto setBalance(BigDecimal v)               { balance = v; return this; }
    public WalletStateDto setCurrentBet(BigDecimal v)            { currentBet = v; return this; }
    public WalletStateDto setLastBet(BigDecimal v)               { lastBet = v; return this; }
    public WalletStateDto setLastWin(BigDecimal v)               { lastWin = v; return this; }
    public WalletStateDto setLastHandWin(BigDecimal v)           { lastHandWin = v; return this; }
    public WalletStateDto setLastPpWin(BigDecimal v)             { lastPpWin = v; return this; }
    public WalletStateDto setLastT3Win(BigDecimal v)             { lastT3Win = v; return this; }
    public WalletStateDto setLastDppWin(BigDecimal v)            { lastDppWin = v; return this; }
    public WalletStateDto setLastTotalBet(BigDecimal v)          { lastTotalBet = v; return this; }
    public WalletStateDto setLastPpBet(BigDecimal v)             { lastPpBet = v; return this; }
    public WalletStateDto setLastT3Bet(BigDecimal v)             { lastT3Bet = v; return this; }
    public WalletStateDto setLastDppBet(BigDecimal v)            { lastDppBet = v; return this; }
    public WalletStateDto setPerfectPairsBet(BigDecimal v)       { perfectPairsBet = v; return this; }
    public WalletStateDto setTwentyOneThreeBet(BigDecimal v)     { twentyOneThreeBet = v; return this; }
    public WalletStateDto setDealerPerfectPairsBet(BigDecimal v) { dealerPerfectPairsBet = v; return this; }
    public WalletStateDto setHandBet(BigDecimal v)               { handBet = v; return this; }
    public WalletStateDto setDoubleBet(BigDecimal v)             { doubleBet = v; return this; }
    public WalletStateDto setInsuranceBet(BigDecimal v)          { insuranceBet = v; return this; }
    public WalletStateDto setSplitBet(BigDecimal v)              { splitBet = v; return this; }
}
