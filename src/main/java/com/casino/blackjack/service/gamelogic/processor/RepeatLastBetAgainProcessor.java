package com.casino.blackjack.service.gamelogic.processor;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_REPEAT_LAST_BET_AGAIN;

/**
 * CHOICE_REPEAT_LAST_BET_AGAIN — player already used repeat-last-bet this turn;
 * nothing to do, just pass through.
 */
public class RepeatLastBetAgainProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_REPEAT_LAST_BET_AGAIN);
    }

    @Override
    public GameContext process(GameContext ctx) {
        return ctx;
    }
}
