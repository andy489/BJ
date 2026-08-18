package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;

public class NotDealtOrFinalizedProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Game game = ctx.game();
        return !game.getDealt() || game.getFinalized();
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        if (game.getLastChoice().equals(CHOICE_DOUBLE_DOWN)) {
            game.setDoubleDown(true);
            game.playerHit();

            if (game.checkBust(game.getPlayerCards())) {
                game.dealerPlayOneCard();
            } else {
                game.dealerPlayUntilSoft17Public();
            }
        }

        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        return ctx;
    }
}
