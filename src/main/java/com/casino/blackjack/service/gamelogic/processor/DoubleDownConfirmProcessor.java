package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

public class DoubleDownConfirmProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Integer last = ctx.game().getLastTakenChoicePublic();
        return last.equals(CHOICE_DOUBLE_DOWN_YES) || last.equals(CHOICE_DOUBLE_DOWN_NO);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        Integer last = game.getLastTakenChoicePublic();

        if (last.equals(CHOICE_DOUBLE_DOWN_YES)) {
            game.setFinalized(true);
            game.setDoubleDown(true);
            game.playerHit();

            if (game.checkBust(game.getPlayerCards())) {
                game.dealerPlayOneCard();
                game.setHandMultiplier(ZERO_MULTI);
            } else {
                game.dealerPlayUntilSoft17Public();
                Integer result = game.compareHands(game.getDealerCards(), game.getPlayerCards());

                if (result < 0) {
                    game.setHandMultiplier(ZERO_MULTI);
                } else if (result == 0) {
                    game.setHandMultiplier(PUSH_MULTI);
                } else {
                    game.setHandMultiplier(DOUBLE_MULTI);
                }
            }

            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            return ctx;
        }

        // CHOICE_DOUBLE_DOWN_NO: cancel confirm, resume normal play
        game.setFinalized(false);
        return ctx;
    }
}
