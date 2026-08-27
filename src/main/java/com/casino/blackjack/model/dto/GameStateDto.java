package com.casino.blackjack.model.dto;

import java.util.List;

public class GameStateDto {

    private String hash;
    private boolean dealt;
    private boolean finalized;
    private boolean splitActive;
    private int activeSplitHandIndex;
    private boolean doubleDown;
    private boolean splitAces;

    private List<CardDto> playerCards;
    private List<CardDto> dealerCards;
    private List<List<CardDto>> splitHands;
    private List<Double> splitHandMultipliers;
    private List<Boolean> splitDoubleDownFlags;

    private String playerScore;
    private String dealerScore;
    private List<String> splitScores;

    private List<Integer> availableChoices;
    private List<Integer> errCodeList;

    private Double handMultiplier;
    private Double insuranceMultiplier;

    private WalletStateDto wallet;

    /** Populated only when a hand finalizes; null otherwise. */
    private List<BetHistoryEntryDto> betHistory;

    /** Non-null only for /accept with depositRedirect=true. */
    private String redirectUrl;

    public GameStateDto() {}

    public String getHash()                             { return hash; }
    public boolean isDealt()                            { return dealt; }
    public boolean isFinalized()                        { return finalized; }
    public boolean isSplitActive()                      { return splitActive; }
    public int getActiveSplitHandIndex()                { return activeSplitHandIndex; }
    public boolean isDoubleDown()                       { return doubleDown; }
    public boolean isSplitAces()                        { return splitAces; }
    public List<CardDto> getPlayerCards()               { return playerCards; }
    public List<CardDto> getDealerCards()               { return dealerCards; }
    public List<List<CardDto>> getSplitHands()          { return splitHands; }
    public List<Double> getSplitHandMultipliers()       { return splitHandMultipliers; }
    public List<Boolean> getSplitDoubleDownFlags()      { return splitDoubleDownFlags; }
    public String getPlayerScore()                      { return playerScore; }
    public String getDealerScore()                      { return dealerScore; }
    public List<String> getSplitScores()                { return splitScores; }
    public List<Integer> getAvailableChoices()          { return availableChoices; }
    public List<Integer> getErrCodeList()               { return errCodeList; }
    public Double getHandMultiplier()                   { return handMultiplier; }
    public Double getInsuranceMultiplier()              { return insuranceMultiplier; }
    public WalletStateDto getWallet()                   { return wallet; }
    public List<BetHistoryEntryDto> getBetHistory()     { return betHistory; }
    public String getRedirectUrl()                      { return redirectUrl; }

    public GameStateDto setHash(String v)                               { hash = v; return this; }
    public GameStateDto setDealt(boolean v)                             { dealt = v; return this; }
    public GameStateDto setFinalized(boolean v)                         { finalized = v; return this; }
    public GameStateDto setSplitActive(boolean v)                       { splitActive = v; return this; }
    public GameStateDto setActiveSplitHandIndex(int v)                  { activeSplitHandIndex = v; return this; }
    public GameStateDto setDoubleDown(boolean v)                        { doubleDown = v; return this; }
    public GameStateDto setSplitAces(boolean v)                         { splitAces = v; return this; }
    public GameStateDto setPlayerCards(List<CardDto> v)                 { playerCards = v; return this; }
    public GameStateDto setDealerCards(List<CardDto> v)                 { dealerCards = v; return this; }
    public GameStateDto setSplitHands(List<List<CardDto>> v)            { splitHands = v; return this; }
    public GameStateDto setSplitHandMultipliers(List<Double> v)         { splitHandMultipliers = v; return this; }
    public GameStateDto setSplitDoubleDownFlags(List<Boolean> v)        { splitDoubleDownFlags = v; return this; }
    public GameStateDto setPlayerScore(String v)                        { playerScore = v; return this; }
    public GameStateDto setDealerScore(String v)                        { dealerScore = v; return this; }
    public GameStateDto setSplitScores(List<String> v)                  { splitScores = v; return this; }
    public GameStateDto setAvailableChoices(List<Integer> v)            { availableChoices = v; return this; }
    public GameStateDto setErrCodeList(List<Integer> v)                 { errCodeList = v; return this; }
    public GameStateDto setHandMultiplier(Double v)                     { handMultiplier = v; return this; }
    public GameStateDto setInsuranceMultiplier(Double v)                { insuranceMultiplier = v; return this; }
    public GameStateDto setWallet(WalletStateDto v)                     { wallet = v; return this; }
    public GameStateDto setBetHistory(List<BetHistoryEntryDto> v)       { betHistory = v; return this; }
    public GameStateDto setRedirectUrl(String v)                        { redirectUrl = v; return this; }
}
