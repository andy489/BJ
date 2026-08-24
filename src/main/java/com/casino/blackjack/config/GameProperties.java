package com.casino.blackjack.config;

import com.casino.blackjack.service.gamelogic.rng.scenario.DeckScenario;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "game.blackjack")
public class GameProperties {

    private int maxSplits = 4;

    private boolean showCardShuffler = false;

    private DeckScenario deckScenario = DeckScenario.RANDOM;

    private int resultDisplayMs = 2500;

    private int dealerRevealMs = 500;

    public int getMaxSplits() {
        return maxSplits;
    }

    public void setMaxSplits(int maxSplits) {
        this.maxSplits = maxSplits;
    }

    public boolean isShowCardShuffler() {
        return showCardShuffler;
    }

    public void setShowCardShuffler(boolean showCardShuffler) {
        this.showCardShuffler = showCardShuffler;
    }

    public DeckScenario getDeckScenario() {
        return deckScenario;
    }

    public void setDeckScenario(DeckScenario deckScenario) {
        this.deckScenario = deckScenario;
    }

    public int getResultDisplayMs() {
        return resultDisplayMs;
    }

    public void setResultDisplayMs(int resultDisplayMs) {
        this.resultDisplayMs = resultDisplayMs;
    }

    public int getDealerRevealMs() {
        return dealerRevealMs;
    }

    public void setDealerRevealMs(int dealerRevealMs) {
        this.dealerRevealMs = dealerRevealMs;
    }
}
