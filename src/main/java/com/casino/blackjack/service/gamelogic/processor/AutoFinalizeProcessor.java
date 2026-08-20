package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_FINALIZE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DEALER_THRESHOLD_17;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

/**
 * Plays out all remaining split hands automatically using dealer-like logic:
 * hit until the effective score >= 17, then stand. Applies to the current
 * active hand and every subsequent hand in the split sequence.
 */
public class AutoFinalizeProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_AUTO_FINALIZE);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        while (game.getSplitActive()) {
            // If the current hand was already doubled (dd card dealt, no further hits allowed),
            // skip playing it and advance directly.
            boolean alreadyDoubled = Boolean.TRUE.equals(game.getDoubleDown());
            if (!alreadyDoubled) {
                playHandAutomatically(game);
            }

            Count count = game.getCount(game.getPlayerCards());
            boolean busted = count.getLeft() > BJ_CNT;
            double multiplier = busted ? ZERO_MULTI : 0.0;

            boolean moreHands = game.advanceSplitHand(multiplier, alreadyDoubled || game.getDoubleDown());
            if (!moreHands) {
                game.dealerPlayUntilSoft17Public();
                SplitHandHelper.resolveAndFinalize(game);
                return ctx;
            }
        }

        return ctx;
    }

    private void playHandAutomatically(Game game) {
        while (true) {
            Count count = game.getCount(game.getPlayerCards());
            int hardScore = count.getLeft();
            int softScore = count.getRight();

            if (hardScore > BJ_CNT) {
                break;
            }

            int effectiveScore = softScore <= BJ_CNT ? softScore : hardScore;
            if (effectiveScore >= DEALER_THRESHOLD_17) {
                break;
            }

            game.playerHit();
        }
    }
}
