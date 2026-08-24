package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.service.gamelogic.dto.Card;

import java.util.Arrays;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CLUBS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DIAMONDS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.HEARTS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SPADES_SUIT;

/**
 * Evaluates Perfect Pairs and 21+3 side bets.
 *
 * Multipliers use total-return convention (bet returned + winnings):
 *   Perfect Pairs: Mixed 7:1 → 8.0, Coloured 12:1 → 13.0, Perfect 25:1 → 26.0
 *   21+3: Flush 5:1 → 6.0, Straight 10:1 → 11.0, Three-of-a-Kind 30:1 → 31.0,
 *         Straight Flush 40:1 → 41.0, Suited Three-of-a-Kind 100:1 → 101.0
 *   No win: 0.0
 */
public class SideBetEvaluator {

    // Perfect Pairs multipliers (total-return) — Mixed 5:1, Coloured 10:1, Perfect 30:1
    public static final double PP_MIXED_MULTI       = 6.0;   // 5:1
    public static final double PP_COLOURED_MULTI    = 11.0;  // 10:1
    public static final double PP_PERFECT_MULTI     = 31.0;  // 30:1

    // 21+3 multipliers (total-return)
    public static final double T3_FLUSH_MULTI       = 6.0;   // 5:1
    public static final double T3_STRAIGHT_MULTI    = 11.0;  // 10:1
    public static final double T3_THREE_KIND_MULTI  = 31.0;  // 30:1
    public static final double T3_STR_FLUSH_MULTI   = 41.0;  // 40:1
    public static final double T3_SUITED_THREE_MULTI = 101.0; // 100:1

    /**
     * Perfect Pairs: evaluated on the player's first two cards only.
     * Returns the total-return multiplier, or 0.0 if no pair.
     */
    public static double evalPerfectPairs(Card c0, Card c1) {
        if (c0.getRank() == null || c1.getRank() == null) return 0.0;
        if (!c0.getRank().equals(c1.getRank())) return 0.0;

        if (c0.getSuit().equals(c1.getSuit())) {
            return PP_PERFECT_MULTI;
        }
        if (sameColour(c0.getSuit(), c1.getSuit())) {
            return PP_COLOURED_MULTI;
        }
        return PP_MIXED_MULTI;
    }

    /**
     * 21+3: player card 0, player card 1, dealer up-card.
     * Returns the total-return multiplier, or 0.0 if no qualifying hand.
     */
    public static double eval21_3(Card p0, Card p1, Card dealer) {
        if (p0.getRank() == null || p1.getRank() == null || dealer.getRank() == null) return 0.0;

        int r0 = bjRank(p0.getRank());
        int r1 = bjRank(p1.getRank());
        int rd = bjRank(dealer.getRank());

        int s0 = p0.getSuit();
        int s1 = p1.getSuit();
        int sd = dealer.getSuit();

        boolean sameSuit = s0 == s1 && s1 == sd;
        boolean sameRank = r0 == r1 && r1 == rd;

        // Suited Three-of-a-Kind (same suit + same rank) — highest, check first
        if (sameSuit && sameRank) return T3_SUITED_THREE_MULTI;

        // Straight Flush (same suit + sequential rank)
        if (sameSuit && isStraight(r0, r1, rd)) return T3_STR_FLUSH_MULTI;

        // Three-of-a-Kind (same rank, any suits)
        if (sameRank) return T3_THREE_KIND_MULTI;

        // Straight (sequential rank, any suits)
        if (isStraight(r0, r1, rd)) return T3_STRAIGHT_MULTI;

        // Flush (same suit, not already caught above)
        if (sameSuit) return T3_FLUSH_MULTI;

        return 0.0;
    }

    private static boolean isStraight(int r0, int r1, int r2) {
        int[] sorted = {r0, r1, r2};
        Arrays.sort(sorted);
        // Normal straight: consecutive ranks
        if (sorted[1] == sorted[0] + 1 && sorted[2] == sorted[1] + 1) return true;
        // Ace-high wrap: A(1), Q(10), K(10) — not possible with card model (no wrap needed)
        // A(1), 2(2), 3(3) — handled by normal consecutive check
        return false;
    }

    /** Blackjack point rank: 10/J/Q/K all map to 10 for straight detection purposes.
     *  For 21+3 straights we use the actual rank value since J=11, Q=12, K=13 are distinct. */
    private static int bjRank(int rank) {
        return rank; // keep full rank for straight detection (10,J,Q,K are distinct)
    }

    private static boolean sameColour(int s0, int s1) {
        boolean s0Red = s0 == DIAMONDS_SUIT || s0 == HEARTS_SUIT;
        boolean s1Red = s1 == DIAMONDS_SUIT || s1 == HEARTS_SUIT;
        return s0Red == s1Red;
    }

    private SideBetEvaluator() {}
}
