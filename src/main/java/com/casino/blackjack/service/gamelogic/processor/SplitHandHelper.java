package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PENDING_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

/**
 * Shared logic for advancing through split hands and resolving dealer play
 * when all hands have been completed.
 */
final class SplitHandHelper {

    private SplitHandHelper() {}

    /**
     * Called when the current active hand has been resolved during a split.
     * Advances to the next hand, or finalizes the round if all hands are done.
     *
     * splitHands[0] = main hand, splitHands[1+] = additional split hands.
     * splitHandMultipliers[i] = result multiplier for splitHands[i].
     */
    static void advanceOrFinalize(Game game, double multiplier, boolean wasDouble) {
        advanceOrFinalize(game, multiplier, wasDouble, Integer.MAX_VALUE, true);
    }

    static void advanceOrFinalize(Game game, double multiplier, boolean wasDouble, int maxSplits) {
        advanceOrFinalize(game, multiplier, wasDouble, maxSplits, true);
    }

    static void advanceOrFinalize(GameContext ctx, double multiplier, boolean wasDouble, int maxSplits) {
        boolean canAffordSplit = ctx.walletEntity().getBalance()
                .compareTo(ctx.walletEntity().getHandBet()) >= 0;
        advanceOrFinalize(ctx.game(), multiplier, wasDouble, maxSplits, canAffordSplit);
    }

    private static void advanceOrFinalize(Game game, double multiplier, boolean wasDouble,
                                          int maxSplits, boolean canAffordSplit) {
        boolean moreHands = game.advanceSplitHand(multiplier, wasDouble);

        if (moreHands) {
            // If the next hand is already 21 after the initial deal, auto-advance it too
            Count nextCount = game.getCount(game.getActiveHandCards());
            if (nextCount.getRight().equals(BJ_CNT)) {
                advanceOrFinalize(game, PENDING_MULTI, false, maxSplits, canAffordSplit);
            } else {
                SplitBetProcessor.setChoicesForActiveHand(game, game.getSplitAces(), maxSplits, canAffordSplit);
            }
        } else {
            game.dealerPlayUntilSoft17Public();
            resolveAllSplitHands(game);
            game.setSplitActive(false);
            game.setFinalized(true);
            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        }
    }

    /** Called by AutoFinalizeProcessor after dealer has already played and all hands are resolved. */
    static void resolveAndFinalize(Game game) {
        resolveAllSplitHands(game);
        game.setSplitActive(false);
        game.setFinalized(true);
        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
    }

    /**
     * After dealer plays, re-compute the final multiplier for every split hand.
     * splitHands[0] = main hand, splitHands[1+] = split hands.
     * Results are written back into splitHandMultipliers.
     * game.handMultiplier is set to the main hand (splitHands[0]) result.
     *
     * Hands carrying PENDING_MULTI (immediate-21 hands skipped during play) also have their
     * multiplier computed here from the final score comparison.
     */
    private static void resolveAllSplitHands(Game game) {
        List<List<Card>> splitHands = game.getSplitHands();
        List<Double> multipliers = game.getSplitHandMultipliers();
        List<Boolean> ddFlags = game.getSplitDoubleDownFlags();

        for (int i = 0; i < splitHands.size(); i++) {
            // Always recompute — this handles PENDING_MULTI and corrects any stale 0.0 values.
            double m = computeMultiplier(game, splitHands.get(i));
            multipliers.set(i, m);
        }

        // Reflect main hand result in handMultiplier
        if (!splitHands.isEmpty()) {
            game.setHandMultiplier(multipliers.get(0));
            game.setDoubleDown(ddFlags.get(0));
        }
    }

    static double computeMultiplier(Game game, List<Card> hand) {
        if (game.checkBust(hand)) {
            return ZERO_MULTI;
        }

        List<Card> dealer = game.getDealerCards();
        Count dealerCount = game.getCount(dealer);
        Count playerCount = game.getCount(hand);

        int dealerScore = dealerCount.getRight() > BJ_CNT ? dealerCount.getLeft() : dealerCount.getRight();
        int playerScore = playerCount.getRight() > BJ_CNT ? playerCount.getLeft() : playerCount.getRight();

        if (dealerScore > BJ_CNT) {
            return DOUBLE_MULTI;
        }

        int cmp = Integer.compare(dealerScore, playerScore);
        if (cmp < 0) {
            return DOUBLE_MULTI;
        } else if (cmp == 0) {
            return PUSH_MULTI;
        } else {
            return ZERO_MULTI;
        }
    }
}
