package com.casino.blackjack.service.simulation;

import com.casino.blackjack.config.PaytableProperties;

/**
 * Exact analytical RTP for Perfect Pairs and 21+3 under infinite-deck assumption.
 * Multipliers are read from {@link PaytableProperties}.
 *
 * Infinite deck: each card drawn independently.
 *   P(rank = r)  = 1/13, P(suit = s) = 1/4
 */
public final class SideBetRtpCalculator {

    public static double perfectPairsRtp(PaytableProperties pt) {
        double pPair     = 1.0 / 13.0;
        double pPerfect  = 1.0 / 4.0;   // given pair: same suit
        double pColoured = 1.0 / 4.0;   // given pair: same colour, diff suit
        double pMixed    = 1.0 / 2.0;   // given pair: diff colour

        double expectedReturn = pPair * (
                pPerfect  * pt.ppPerfectMulti()  +
                pColoured * pt.ppColouredMulti() +
                pMixed    * pt.ppMixedMulti()
        );
        return expectedReturn * 100.0;
    }

    public static double twentyOneThreeRtp(PaytableProperties pt) {
        double pSameRank3      = 1.0 / (13.0 * 13.0);
        double pSameSuit3      = 1.0 / (4.0  *  4.0);
        double pStraightRanks  = 66.0 / (13.0 * 13.0 * 13.0);

        double pSuited3ok  = pSameRank3 * pSameSuit3;
        double pSF         = pStraightRanks * pSameSuit3;
        double p3ok        = pSameRank3 * (1.0 - pSameSuit3);
        double pStraight   = pStraightRanks * (1.0 - pSameSuit3);
        double pFlush      = pSameSuit3 - pSF - pSuited3ok;

        double expectedReturn =
                pSuited3ok * pt.t3SuitedThreeMulti()   +
                pSF        * pt.t3StraightFlushMulti()  +
                p3ok       * pt.t3ThreeOfAKindMulti()   +
                pStraight  * pt.t3StraightMulti()       +
                pFlush     * pt.t3FlushMulti();

        return expectedReturn * 100.0;
    }

    public record SideBetRtpResult(double ppRtp, double t3Rtp) {}

    public static SideBetRtpResult calculate(PaytableProperties pt) {
        return new SideBetRtpResult(perfectPairsRtp(pt), twentyOneThreeRtp(pt));
    }

    private SideBetRtpCalculator() {}
}
