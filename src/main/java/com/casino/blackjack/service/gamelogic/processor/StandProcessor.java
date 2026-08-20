package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

public class StandProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_STAND);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        if (game.getSplitActive()) {
            SplitHandHelper.advanceOrFinalize(ctx, 0.0, game.getDoubleDown(), ctx.maxSplits());
            return ctx;
        }

        game.setFinalized(true);
        game.dealerPlayUntilSoft17Public();

        Count dealerCount = game.getCount(game.getDealerCards());
        Count playerCount = game.getCount(game.getPlayerCards());

        int dealerScore = dealerCount.getRight();
        int playerScore = playerCount.getRight() > BJ_CNT
                ? playerCount.getLeft()
                : playerCount.getRight();

        if (dealerScore > BJ_CNT) {
            game.setHandMultiplier(DOUBLE_MULTI);
        } else {
            int cmp = Integer.compare(dealerScore, playerScore);
            if (cmp < 0) {
                game.setHandMultiplier(DOUBLE_MULTI);
            } else if (cmp == 0) {
                game.setHandMultiplier(PUSH_MULTI);
            } else {
                game.setHandMultiplier(ZERO_MULTI);
            }
        }

        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        return ctx;
    }
}
