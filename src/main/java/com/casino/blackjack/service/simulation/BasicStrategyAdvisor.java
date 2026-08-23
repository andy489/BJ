package com.casino.blackjack.service.simulation;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Count;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;

/**
 * Stateless basic-strategy advisor for the simulation engine.
 * Rules: S17 (dealer stands soft 17), no surrender, re-splits allowed,
 * double after split allowed. Standard multi-deck basic strategy.
 */
public final class BasicStrategyAdvisor {

    public enum Action { HIT, STAND, DOUBLE, SPLIT }

    private BasicStrategyAdvisor() {}

    /**
     * Returns the basic-strategy action for the given player hand against
     * the dealer's upcard (first visible card).
     *
     * @param playerCards  current player cards
     * @param dealerUpcard dealer's face-up card rank (1–13; face cards = 10)
     * @param isFirstAction true if no cards have been added since deal (doubles/splits allowed)
     */
    public static Action advise(List<Card> playerCards, int dealerUpcard, boolean isFirstAction) {
        int upcard = normalizeRank(dealerUpcard);  // 1(Ace)–10

        // ── Pair splitting (only on initial two cards) ──
        if (isFirstAction && playerCards.size() == 2) {
            int r1 = normalizeRank(playerCards.get(0).getRank());
            int r2 = normalizeRank(playerCards.get(1).getRank());
            if (r1 == r2) {
                Action split = splitAdvice(r1, upcard);
                if (split == Action.SPLIT) return Action.SPLIT;
            }
        }

        Count count = computeCount(playerCards);
        int hard = count.getLeft();
        int soft = count.getRight();
        boolean hasSoftAce = (soft != hard && soft <= BJ_CNT);

        // ── Soft totals (hand has usable Ace) ──
        if (hasSoftAce) {
            return softAdvice(soft, upcard, isFirstAction);
        }

        // ── Hard totals ──
        return hardAdvice(hard, upcard, isFirstAction);
    }

    // ── Hard total basic strategy ──
    private static Action hardAdvice(int total, int upcard, boolean canDouble) {
        if (total >= 17) return Action.STAND;
        if (total <= 8)  return Action.HIT;

        if (total == 9) {
            if (canDouble && upcard >= 3 && upcard <= 6) return Action.DOUBLE;
            return Action.HIT;
        }
        if (total == 10) {
            if (canDouble && upcard >= 2 && upcard <= 9) return Action.DOUBLE;
            return Action.HIT;
        }
        if (total == 11) {
            if (canDouble && upcard >= 2 && upcard <= 10) return Action.DOUBLE;
            return Action.HIT;
        }
        if (total == 12) {
            if (upcard >= 4 && upcard <= 6) return Action.STAND;
            return Action.HIT;
        }
        if (total >= 13 && total <= 16) {
            if (upcard >= 2 && upcard <= 6) return Action.STAND;
            return Action.HIT;
        }
        return Action.HIT;
    }

    // ── Soft total basic strategy ──
    private static Action softAdvice(int soft, int upcard, boolean canDouble) {
        // soft = best non-bust total with Ace counted as 11
        switch (soft) {
            case 13: case 14: // A+2, A+3
                if (canDouble && upcard >= 5 && upcard <= 6) return Action.DOUBLE;
                return Action.HIT;
            case 15: case 16: // A+4, A+5
                if (canDouble && upcard >= 4 && upcard <= 6) return Action.DOUBLE;
                return Action.HIT;
            case 17: // A+6
                if (canDouble && upcard >= 3 && upcard <= 6) return Action.DOUBLE;
                return Action.HIT;
            case 18: // A+7
                if (canDouble && upcard >= 3 && upcard <= 6) return Action.DOUBLE;
                if (upcard >= 9) return Action.HIT;   // 9, 10, Ace
                return Action.STAND;
            case 19: // A+8
                if (canDouble && upcard == 6) return Action.DOUBLE;
                return Action.STAND;
            default: // soft 20+
                return Action.STAND;
        }
    }

    // ── Pair splitting basic strategy ──
    private static Action splitAdvice(int pairRank, int upcard) {
        switch (pairRank) {
            case 1:  return Action.SPLIT;  // Always split Aces
            case 8:  return Action.SPLIT;  // Always split 8s
            case 9:
                // Split 9s vs 2–6, 8–9; stand vs 7, 10, Ace
                if (upcard == 7 || upcard == 10 || upcard == 1) return Action.STAND;
                return Action.SPLIT;
            case 7:
                return (upcard >= 2 && upcard <= 7) ? Action.SPLIT : Action.HIT;
            case 6:
                return (upcard >= 2 && upcard <= 6) ? Action.SPLIT : Action.HIT;
            case 4:
                return (upcard == 5 || upcard == 6) ? Action.SPLIT : Action.HIT;
            case 3: case 2:
                return (upcard >= 2 && upcard <= 7) ? Action.SPLIT : Action.HIT;
            case 5:  // Never split 5s — treat as hard 10
            case 10: // Never split 10s
            default:
                return Action.STAND; // signal: don't split
        }
    }

    /** Normalize face cards (J=11, Q=12, K=13) to 10; Ace stays 1. */
    public static int normalizeRank(int rank) {
        return rank >= TEN_RANK ? TEN_RANK : rank;
    }

    /** Recompute Count without relying on Game instance. */
    public static Count computeCount(List<Card> cards) {
        int left = 0, right = 0;
        for (Card c : cards) {
            int r = c.getRank();
            if (r == ACE_RANK) {
                left += 1;
                right = (right + 11 <= BJ_CNT) ? right + 11 : right + 1;
            } else {
                int v = r >= TEN_RANK ? 10 : r;
                left += v;
                right += v;
            }
        }
        if (right > BJ_CNT) right = left;
        return Count.of(left, right);
    }
}
