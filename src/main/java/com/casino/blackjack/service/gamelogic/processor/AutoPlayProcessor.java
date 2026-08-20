package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_PLAY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DEALER_THRESHOLD_17;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

public class AutoPlayProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_AUTO_PLAY);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        while (true) {
            Count count = game.getCount(game.getPlayerCards());
            int hardScore = count.getLeft();
            int softScore = count.getRight();

            if (hardScore > BJ_CNT) {
                game.setFinalized(true);
                game.setHandMultiplier(ZERO_MULTI);
                game.dealerPlayOneCard();
                game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
                return ctx;
            }

            int effectiveScore = softScore <= BJ_CNT ? softScore : hardScore;
            if (effectiveScore >= DEALER_THRESHOLD_17) {
                break;
            }

            game.playerHit();
        }

        Count finalCount = game.getCount(game.getPlayerCards());
        if (finalCount.getRight().equals(BJ_CNT)) {
            game.dealerPlayUntilSoft17Public();
            game.setFinalized(true);
            Count dealerCount = game.getCount(game.getDealerCards());
            game.setHandMultiplier(dealerCount.getRight().equals(BJ_CNT) ? PUSH_MULTI : DOUBLE_MULTI);
            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            return ctx;
        }

        game.dealerPlayUntilSoft17Public();
        game.setFinalized(true);

        Count playerCount = game.getCount(game.getPlayerCards());
        Count dealerCount = game.getCount(game.getDealerCards());

        int playerScore = playerCount.getRight() <= BJ_CNT ? playerCount.getRight() : playerCount.getLeft();
        int dealerScore = dealerCount.getRight() <= BJ_CNT ? dealerCount.getRight() : dealerCount.getLeft();

        if (dealerScore > BJ_CNT || playerScore > dealerScore) {
            game.setHandMultiplier(DOUBLE_MULTI);
        } else if (playerScore == dealerScore) {
            game.setHandMultiplier(PUSH_MULTI);
        } else {
            game.setHandMultiplier(ZERO_MULTI);
        }

        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        return ctx;
    }
}
