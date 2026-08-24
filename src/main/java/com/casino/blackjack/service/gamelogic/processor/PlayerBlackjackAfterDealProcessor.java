package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_YES;

public class PlayerBlackjackAfterDealProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Game game = ctx.game();
        return game.getLastTakenChoicePublic().equals(
                com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL)
                && game.checkBJCards(game.getPlayerCards());
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        if (game.dealerFirstCardCannotMakeBJ()) {
            game.dealerPlayOneCard();
            game.setFinalized(true);
            game.setHandMultiplier(ctx.paytable().bjMulti());
            game.setAvailableChoices(List.of(CHOICE_DEAL));
            return ctx;
        }

        game.setAvailableChoices(List.of(CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO));
        return ctx;
    }
}
