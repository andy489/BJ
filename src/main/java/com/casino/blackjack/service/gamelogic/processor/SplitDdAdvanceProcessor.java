package com.casino.blackjack.service.gamelogic.processor;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT_DD_ADVANCE;

/**
 * Advances to the next split hand after a double-down on a split hand.
 * Unlike AutoFinalizeProcessor (which loops through all remaining hands),
 * this processor advances exactly one hand and stops, returning control to the player.
 */
public class SplitDdAdvanceProcessor implements GameStateProcessor {

    private final int maxSplits;

    public SplitDdAdvanceProcessor(int maxSplits) {
        this.maxSplits = maxSplits;
    }

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_SPLIT_DD_ADVANCE);
    }

    @Override
    public GameContext process(GameContext ctx) {
        SplitHandHelper.advanceOrFinalize(ctx, 0.0, true, maxSplits);
        return ctx;
    }
}
