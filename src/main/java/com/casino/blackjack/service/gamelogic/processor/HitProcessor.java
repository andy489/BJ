package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

public class HitProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_HIT);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        game.playerHit();
        Count playerCount = game.getCount(game.getPlayerCards());

        if (playerCount.getRight().equals(BJ_CNT)) {
            game.dealerPlayUntilSoft17Public();
            game.setFinalized(true);

            Count dealerCount = game.getCount(game.getDealerCards());
            if (dealerCount.getRight().equals(BJ_CNT)) {
                game.setHandMultiplier(PUSH_MULTI);
            } else {
                game.setHandMultiplier(DOUBLE_MULTI);
            }

            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            return ctx;
        }

        if (playerCount.getLeft() > BJ_CNT) {
            game.setFinalized(true);
            game.setHandMultiplier(ZERO_MULTI);
            game.dealerPlayOneCard();
            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            return ctx;
        }

        game.setAvailableChoices(List.of(CHOICE_STAND, CHOICE_HIT));
        return ctx;
    }
}
