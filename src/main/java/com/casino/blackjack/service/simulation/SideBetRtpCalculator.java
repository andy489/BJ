package com.casino.blackjack.service.simulation;

import com.casino.blackjack.config.PaytableProperties;

/**
 * Exact analytical RTP for Perfect Pairs and 21+3.
 *
 * Infinite deck: cards drawn with replacement — each draw is independent.
 * N-deck: cards drawn without replacement from N×52 cards.
 */
public final class SideBetRtpCalculator {

    // ── Infinite-deck ────────────────────────────────────────────────────────

    /**
     * PP infinite-deck RTP.
     *
     * For any first card (e.g. A♠) the 52 equally likely second cards split as:
     *   1/52 → Perfect  (same rank, same suit)
     *   1/52 → Coloured (same rank, same colour, different suit)
     *   2/52 → Mixed    (same rank, different colour)
     *  48/52 → No pair  → 0 return
     *
     * RTP = (1/52)×ppPerfect + (1/52)×ppColoured + (2/52)×ppMixed
     */
    public static double perfectPairsRtp(PaytableProperties pt) {
        double rtp = (1.0/52) * pt.getPpPerfect()
                   + (1.0/52) * pt.getPpColoured()
                   + (2.0/52) * pt.getPpMixed();
        return rtp * 100.0;
    }

    /**
     * 21+3 infinite-deck RTP.
     *
     * Probabilities under with-replacement (infinite deck):
     *   P(same rank, 3 cards)  = 1/13²
     *   P(same suit, 3 cards)  = 1/4²  = 1/16
     *   P(straight ranks, ordered 3) = 72/13³  (12 sets × 3! orderings)
     */
    public static double twentyOneThreeRtp(PaytableProperties pt) {
        double pSameRank3     = 1.0 / (13.0 * 13.0);
        double pSameSuit3     = 1.0 / (4.0  *  4.0);
        double pStraightRanks = 72.0 / (13.0 * 13.0 * 13.0);

        double pSuited3ok = pSameRank3 * pSameSuit3;
        double pSF        = pStraightRanks * pSameSuit3;
        double p3ok       = pSameRank3 * (1.0 - pSameSuit3);
        double pStraight  = pStraightRanks * (1.0 - pSameSuit3);
        double pFlush     = pSameSuit3 - pSF - pSuited3ok;

        double expectedReturn =
                pSuited3ok * pt.t3SuitedThreeMulti()  +
                pSF        * pt.t3StraightFlushMulti() +
                p3ok       * pt.t3ThreeOfAKindMulti()  +
                pStraight  * pt.t3StraightMulti()      +
                pFlush     * pt.t3FlushMulti();

        return expectedReturn * 100.0;
    }

    // ── N-deck ───────────────────────────────────────────────────────────────

    /**
     * PP RTP for N standard decks (without replacement).
     *
     * After drawing any first card, N×52−1 cards remain:
     *   Perfect  : N−1 cards (same rank + same suit)
     *   Coloured : N   cards (same rank, same colour, other suit)
     *   Mixed    : 2N  cards (same rank, 2 opposite-colour suits, N each)
     */
    public static double perfectPairsRtpNDeck(int n, PaytableProperties pt) {
        double remain   = (double)(n * 52 - 1);
        double perfect  = n - 1;
        double coloured = n;
        double mixed    = 2.0 * n;
        double rtp = (perfect  * pt.getPpPerfect()
                    + coloured * pt.getPpColoured()
                    + mixed    * pt.getPpMixed()) / remain;
        return rtp * 100.0;
    }

    /**
     * 21+3 RTP for N standard decks (ordered triples, without replacement).
     *
     * Total ordered triples = T = (52N)(52N−1)(52N−2)
     *
     * Suited 3oK: 52 rank+suit combos × N(N−1)(N−2) ordered draws from that pile
     * Straight Flush: 12 rank-triples × 4 suits × 3! × N³ (one card from each rank-suit pile)
     * 3oK (excl S3K): 13 ranks × [4N(4N−1)(4N−2) − 4·N(N−1)(N−2)]
     * Straight (excl SF): 12 × 3! × (4N)³ − SF count
     * Flush (excl SF, S3K): 4 suits × [13N(13N−1)(13N−2) − SF/suit − S3K/suit]
     */
    public static double twentyOneThreeRtpNDeck(int n, PaytableProperties pt) {
        long total      = (long)(52*n) * (52*n - 1) * (52*n - 2);

        long s3k     = 52L * n * (n-1) * (n-2);
        long sf      = 12L * 4 * 6 * (long)(n*n*n);  // 12 rank-triples * 4 suits * 3! * N³
        long tok_all = 13L * (4L*n) * (4L*n - 1) * (4L*n - 2);
        long tok     = tok_all - s3k;
        long str_all = 12L * 6 * (long)(4*n) * (4*n) * (4*n);
        long str     = str_all - sf;

        long suit_total  = (long)(13*n) * (13*n - 1) * (13*n - 2);
        long sf_per_suit = 12L * 6 * (long)(n*n*n);
        long s3k_per_suit= 13L * n * (n-1) * (n-2);
        long flush       = 4L * (suit_total - sf_per_suit - s3k_per_suit);

        double rtp = ((double)s3k  * pt.t3SuitedThreeMulti()  +
                      (double)sf   * pt.t3StraightFlushMulti() +
                      (double)tok  * pt.t3ThreeOfAKindMulti()  +
                      (double)str  * pt.t3StraightMulti()      +
                      (double)flush* pt.t3FlushMulti()) / total;

        return rtp * 100.0;
    }

    // ── Result record ────────────────────────────────────────────────────────

    public record SideBetRtpResult(
            double ppRtp, double t3Rtp,
            double ppRtp6, double t3Rtp6,
            double ppRtp8, double t3Rtp8,
            double ppPerfect, double ppColoured, double ppMixed,
            double t3SuitedThree, double t3StraightFlush,
            double t3ThreeOfAKind, double t3Straight, double t3Flush
    ) {}

    public static SideBetRtpResult calculate(PaytableProperties pt) {
        return new SideBetRtpResult(
                perfectPairsRtp(pt),
                twentyOneThreeRtp(pt),
                perfectPairsRtpNDeck(6, pt),
                twentyOneThreeRtpNDeck(6, pt),
                perfectPairsRtpNDeck(8, pt),
                twentyOneThreeRtpNDeck(8, pt),
                pt.getPpPerfect(), pt.getPpColoured(), pt.getPpMixed(),
                pt.getT3SuitedThreeOfAKind(), pt.getT3StraightFlush(),
                pt.getT3ThreeOfAKind(), pt.getT3Straight(), pt.getT3Flush()
        );
    }

    private SideBetRtpCalculator() {}
}
