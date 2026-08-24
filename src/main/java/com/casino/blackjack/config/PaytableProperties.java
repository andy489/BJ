package com.casino.blackjack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * All payout multipliers for the game, loaded from game.blackjack.paytable.* config.
 * Values are expressed as net-profit ratios (e.g. bj=1.5 means a £10 bet wins £15).
 * Total-return multipliers used in game logic = net-profit + 1.0 where applicable.
 */
@Component
@ConfigurationProperties(prefix = "game.blackjack.paytable")
public class PaytableProperties {

    // ── Main hand ──────────────────────────────────────────────────────────
    /** Blackjack payout ratio (net profit per unit bet). Standard: 1.5 (3:2). */
    private double bj = 1.5;

    /** Insurance payout ratio (net profit per unit insurance bet). Standard: 2.0 (2:1). */
    private double insurance = 2.0;

    /** Surrender return ratio (fraction of bet returned). Standard: 0.5 (half-back). */
    private double surrender = 0.5;

    // ── Perfect Pairs ──────────────────────────────────────────────────────
    /** Mixed pair net-profit ratio. E.g. 5 means 5:1 payout. */
    private double ppMixed = 5.0;

    /** Coloured pair net-profit ratio. */
    private double ppColoured = 10.0;

    /** Perfect pair net-profit ratio. */
    private double ppPerfect = 30.0;

    // ── 21+3 ───────────────────────────────────────────────────────────────
    private double t3Flush = 5.0;
    private double t3Straight = 10.0;
    private double t3ThreeOfAKind = 30.0;
    private double t3StraightFlush = 40.0;
    private double t3SuitedThreeOfAKind = 100.0;

    // ── Derived total-return multipliers (used directly in payout math) ──
    /** Total-return multiplier = net-profit ratio + 1.0 (bet returned + winnings). */
    public double bjMulti()               { return bj + 1.0; }
    public double insuranceMulti()        { return insurance + 1.0; }
    public double surrenderMulti()        { return surrender; }  // fraction returned, not +1
    public double ppMixedMulti()          { return ppMixed + 1.0; }
    public double ppColouredMulti()       { return ppColoured + 1.0; }
    public double ppPerfectMulti()        { return ppPerfect + 1.0; }
    public double t3FlushMulti()          { return t3Flush + 1.0; }
    public double t3StraightMulti()       { return t3Straight + 1.0; }
    public double t3ThreeOfAKindMulti()   { return t3ThreeOfAKind + 1.0; }
    public double t3StraightFlushMulti()  { return t3StraightFlush + 1.0; }
    public double t3SuitedThreeMulti()    { return t3SuitedThreeOfAKind + 1.0; }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public double getBj()                       { return bj; }
    public void setBj(double bj)                { this.bj = bj; }

    public double getInsurance()                { return insurance; }
    public void setInsurance(double insurance)  { this.insurance = insurance; }

    public double getSurrender()                { return surrender; }
    public void setSurrender(double surrender)  { this.surrender = surrender; }

    public double getPpMixed()                  { return ppMixed; }
    public void setPpMixed(double ppMixed)      { this.ppMixed = ppMixed; }

    public double getPpColoured()               { return ppColoured; }
    public void setPpColoured(double v)         { this.ppColoured = v; }

    public double getPpPerfect()                { return ppPerfect; }
    public void setPpPerfect(double v)          { this.ppPerfect = v; }

    public double getT3Flush()                  { return t3Flush; }
    public void setT3Flush(double v)            { this.t3Flush = v; }

    public double getT3Straight()               { return t3Straight; }
    public void setT3Straight(double v)         { this.t3Straight = v; }

    public double getT3ThreeOfAKind()           { return t3ThreeOfAKind; }
    public void setT3ThreeOfAKind(double v)     { this.t3ThreeOfAKind = v; }

    public double getT3StraightFlush()          { return t3StraightFlush; }
    public void setT3StraightFlush(double v)    { this.t3StraightFlush = v; }

    public double getT3SuitedThreeOfAKind()         { return t3SuitedThreeOfAKind; }
    public void setT3SuitedThreeOfAKind(double v)   { this.t3SuitedThreeOfAKind = v; }
}
