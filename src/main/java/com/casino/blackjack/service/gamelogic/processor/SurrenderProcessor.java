package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SURRENDER_MULTI;

public class SurrenderProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_SURRENDER);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        game.setFinalized(true);
        game.dealerPlayOneCard();
        game.setHandMultiplier(SURRENDER_MULTI);
        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        return ctx;
    }
}
