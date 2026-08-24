package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.config.PaytableProperties;
import com.casino.blackjack.service.gamelogic.dto.Card;

import java.util.Arrays;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CLUBS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DIAMONDS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.HEARTS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SPADES_SUIT;

/**
 * Evaluates Perfect Pairs and 21+3 side bets.
 * Multipliers are read from {@link PaytableProperties} (total-return convention).
 */
public class SideBetEvaluator {

    /**
     * Perfect Pairs: evaluated on the player's first two cards only.
     * Returns the total-return multiplier, or 0.0 if no pair.
     */
    public static double evalPerfectPairs(Card c0, Card c1, PaytableProperties pt) {
        if (c0.getRank() == null || c1.getRank() == null) return 0.0;
        if (!c0.getRank().equals(c1.getRank())) return 0.0;

        if (c0.getSuit().equals(c1.getSuit())) {
            return pt.ppPerfectMulti();
        }
        if (sameColour(c0.getSuit(), c1.getSuit())) {
            return pt.ppColouredMulti();
        }
        return pt.ppMixedMulti();
    }

    /**
     * 21+3: player card 0, player card 1, dealer up-card.
     * Returns the total-return multiplier, or 0.0 if no qualifying hand.
     */
    public static double eval21_3(Card p0, Card p1, Card dealer, PaytableProperties pt) {
        if (p0.getRank() == null || p1.getRank() == null || dealer.getRank() == null) return 0.0;

        int r0 = p0.getRank();
        int r1 = p1.getRank();
        int rd = dealer.getRank();

        int s0 = p0.getSuit();
        int s1 = p1.getSuit();
        int sd = dealer.getSuit();

        boolean sameSuit = s0 == s1 && s1 == sd;
        boolean sameRank = r0 == r1 && r1 == rd;

        if (sameSuit && sameRank) return pt.t3SuitedThreeMulti();
        if (sameSuit && isStraight(r0, r1, rd)) return pt.t3StraightFlushMulti();
        if (sameRank) return pt.t3ThreeOfAKindMulti();
        if (isStraight(r0, r1, rd)) return pt.t3StraightMulti();
        if (sameSuit) return pt.t3FlushMulti();

        return 0.0;
    }

    private static boolean isStraight(int r0, int r1, int r2) {
        int[] sorted = {r0, r1, r2};
        Arrays.sort(sorted);
        return sorted[1] == sorted[0] + 1 && sorted[2] == sorted[1] + 1;
    }

    private static boolean sameColour(int s0, int s1) {
        boolean s0Red = s0 == DIAMONDS_SUIT || s0 == HEARTS_SUIT;
        boolean s1Red = s1 == DIAMONDS_SUIT || s1 == HEARTS_SUIT;
        return s0Red == s1Red;
    }

    private SideBetEvaluator() {}
}
