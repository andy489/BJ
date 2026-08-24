package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;

public class EvenMoneyProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Integer last = ctx.game().getLastTakenChoicePublic();
        return last.equals(CHOICE_EVEN_MONEY_YES) || last.equals(CHOICE_EVEN_MONEY_NO);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        game.dealerPlayOneCard();
        game.setFinalized(true);

        if (game.getLastTakenChoicePublic().equals(CHOICE_EVEN_MONEY_YES)) {
            game.setHandMultiplier(DOUBLE_MULTI);
        } else {
            game.setHandMultiplier(game.checkBJCards(game.getDealerCards()) ? PUSH_MULTI : ctx.paytable().bjMulti());
        }

        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        return ctx;
    }
}
