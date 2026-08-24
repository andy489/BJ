package com.casino.blackjack.service.simulation;

import com.casino.blackjack.config.PaytableProperties;

/**
 * Exact analytical RTP for Perfect Pairs and 21+3 under infinite-deck assumption.
 * Multipliers are read from {@link PaytableProperties}.
 *
 * Infinite deck: second card drawn independently from 52 equally likely values.
 * For any given first card (e.g. A♠) the 52 possible second cards are:
 *   1/52  → same rank, same suit           = Perfect
 *   1/52  → same rank, same colour, diff suit = Coloured  (only 1 such card per rank)
 *   2/52  → same rank, diff colour         = Mixed        (2 such cards per rank)
 *  48/52  → different rank                 = no pair, 0× return
 */
public final class SideBetRtpCalculator {

    public static double perfectPairsRtp(PaytableProperties pt) {
        // Net-profit RTP: probability × net-profit multiplier, summed
        double rtp = (1.0/52) * pt.getPpPerfect()
                   + (1.0/52) * pt.getPpColoured()
                   + (2.0/52) * pt.getPpMixed();
        return rtp * 100.0;
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
