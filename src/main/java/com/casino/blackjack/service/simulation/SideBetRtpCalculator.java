package com.casino.blackjack.service.simulation;

/**
 * Exact analytical RTP for Perfect Pairs and 21+3 under infinite-deck assumption.
 *
 * Infinite deck: each card drawn independently.
 *   P(rank = r)  = 1/13  (13 distinct ranks: A,2…9,10,J,Q,K)
 *   P(suit = s)  = 1/4   (4 suits)
 *   P(rank=r, suit=s) = 1/52
 *
 * Multipliers follow total-return convention (same as SideBetEvaluator):
 *   PP Mixed 7:1 → 8x, Coloured 12:1 → 13x, Perfect 25:1 → 26x
 *   21+3 Flush 5:1→6x, Straight 10:1→11x, ToK 30:1→31x, SF 40:1→41x, SToK 100:1→101x
 */
public final class SideBetRtpCalculator {

    // ---------- Perfect Pairs (2-card hand) ----------

    /**
     * P(same rank) under infinite deck:
     *   Any rank chosen for card1 (prob 1), card2 must match that rank = 1/13.
     *   Total: 1 * 1/13 = 1/13
     *
     * Given same rank:
     *   P(same suit | same rank)    = 1/4  → Perfect Pair  (26x)
     *   P(same colour | same rank, diff suit) = ?
     *     Given same rank, P(same suit) = 1/4, P(diff suit same colour) = 1/4 (e.g. H↔D, C↔S)
     *     Wait — with replacement: suit of card1 is uniform over 4, suit of card2 is uniform over 4.
     *     P(suit1 == suit2) = 1/4
     *     P(same colour, diff suit) = P(both red diff suit) + P(both black diff suit)
     *       = P(H)*P(D) + P(D)*P(H) + P(C)*P(S) + P(S)*P(C)
     *       = 4 * (1/4)(1/4) = 4/16 = 1/4
     *     But we need to subtract P(same suit) from same-colour:
     *       P(same colour) = P(both red) + P(both black) = (2/4)^2 + (2/4)^2 = 1/2
     *       P(same colour, diff suit) = 1/2 - 1/4 = 1/4
     *     P(mixed pair) = P(pair) - P(same colour) = 1 - 1/2 = 1/2
     *
     * Expected return for PP bet of 1:
     *   = P(pair) * [P(perfect|pair)*26 + P(coloured|pair)*13 + P(mixed|pair)*8] + P(no pair)*0
     *   where:
     *     P(pair)   = 1/13
     *     P(perfect | pair)  = 1/4
     *     P(coloured | pair) = 1/4
     *     P(mixed | pair)    = 1/2
     *
     * RTP = E[return] / 1 (bet) * 100
     */
    public static double perfectPairsRtp() {
        double pPair     = 1.0 / 13.0;
        double pPerfect  = 1.0 / 4.0;   // given pair: same suit
        double pColoured = 1.0 / 4.0;   // given pair: same colour, diff suit
        double pMixed    = 1.0 / 2.0;   // given pair: diff colour

        double expectedReturn = pPair * (pPerfect * 26.0 + pColoured * 13.0 + pMixed * 8.0);
        return expectedReturn * 100.0;
    }

    // ---------- 21+3 (3-card hand: player c0, player c1, dealer up-card) ----------

    /**
     * Three cards drawn independently from an infinite deck.
     * For 21+3, suits use {0..3} and ranks use {1..13} (A=1,J=11,Q=12,K=13).
     * Rank-10, J, Q, K are distinct (rank values differ) so straights work correctly.
     *
     * We enumerate probabilities for each hand category:
     *
     * Let N = 13 (distinct ranks), S = 4 (suits).
     * Each card has probability 1/(N*S) = 1/52.
     * For a 3-card combination:
     *
     * Suited Three-of-a-Kind (same rank AND same suit):
     *   P = 1/13 * 1/13 * 1/4 * 1/4
     *     = ranks must all match: P(r1=r2) * P(r2=r3) = (1/13)^2
     *       suits must all match: (1/4)^2
     *   = (1/13)^2 * (1/4)^2
     *
     * Straight Flush (same suit AND sequential rank):
     *   P(same suit for 3 cards) = (1/4)^2
     *   P(sequential rank for 3 cards) = 12/13 * ... enumerate straights
     *   Count of 3-rank straights from {1..13}: A-2-3, 2-3-4, ..., J-Q-K = 11 runs
     *     but order matters (3! = 6 permutations each) and ranks are independent draws:
     *   P(3 cards form a straight in some order) =
     *     (# ordered rank triples that are a straight) / 13^3
     *   Ordered straight triples: 11 straights * 3! = 66 out of 13^3 = 2197
     *   Subtract suited-3ok (already same-rank) which can't be straight: fine, straights require diff ranks
     *   P(straight ranks) = 66/2197
     *   P(straight flush) = (66/2197) * (1/4)^2
     *     minus P(suited 3ok) = 0 (straights have different ranks, 3ok has same rank — disjoint)
     *
     * Three-of-a-Kind (same rank, any suits):
     *   P(same rank) = (1/13)^2
     *   This includes same-suit (suited 3ok) — we keep the hierarchy by checking suited 3ok first,
     *   but for probability calculation here we want the marginal probability of each tier:
     *   P(3ok, not suited) = (1/13)^2 - (1/13)^2*(1/4)^2 = (1/13)^2 * (1 - 1/16)
     *   But we want to compute expected value, so we'll use mutually-exclusive probabilities:
     *   P(suited3ok)   = (1/13)^2 * (1/4)^2
     *   P(sf)          = 66/2197 * (1/16)  [disjoint from suited3ok since diff ranks]
     *   P(3ok_only)    = (1/13)^2 * (1 - 1/16)
     *   P(straight_only)= (66/2197) * (1 - 1/16) - 0 [straight non-suited]
     *   P(flush_only)  = (1/16) * (1 - 66/2197 - 1/13^2)  [same suit but not sf or suited3ok]
     *   P(loss)        = 1 - all above
     */
    public static double twentyOneThreeRtp() {
        // With three i.i.d. draws from 13 ranks × 4 suits:
        double pSameRank3   = 1.0 / (13.0 * 13.0);           // (1/13)^2 = both c1,c2 match c0
        double pSameSuit3   = 1.0 / (4.0 * 4.0);             // (1/4)^2

        // Straight: 11 starting ranks × 3! orderings / 13^3 total ordered rank triples
        double pStraightRanks = 66.0 / (13.0 * 13.0 * 13.0);

        // Mutually exclusive tiers (highest first, each excludes higher tiers):
        double pSuited3ok   = pSameRank3 * pSameSuit3;                     // 101x
        double pSF          = pStraightRanks * pSameSuit3;                  // 41x  (diff rank → disjoint from 3ok)
        double p3ok         = pSameRank3 * (1.0 - pSameSuit3);              // 31x
        double pStraight    = pStraightRanks * (1.0 - pSameSuit3);          // 11x
        // Flush: same suit but NOT straight and NOT suited-3ok
        double pSameSuit3Total = pSameSuit3;                                // P(all same suit)
        double pFlush       = pSameSuit3Total - pSF - pSuited3ok;            // 6x

        double expectedReturn =
                  pSuited3ok * 101.0
                + pSF        *  41.0
                + p3ok       *  31.0
                + pStraight  *  11.0
                + pFlush     *   6.0;

        return expectedReturn * 100.0;
    }

    public record SideBetRtpResult(double ppRtp, double t3Rtp) {}

    public static SideBetRtpResult calculate() {
        return new SideBetRtpResult(perfectPairsRtp(), twentyOneThreeRtp());
    }

    private SideBetRtpCalculator() {}
}
