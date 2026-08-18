package com.casino.blackjack.service.gamelogic.processor;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;

public class DoubleDownProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_DOUBLE_DOWN);
    }

    @Override
    public GameContext process(GameContext ctx) {
        ctx.game().setFinalized(false);
        return ctx;
    }
}
