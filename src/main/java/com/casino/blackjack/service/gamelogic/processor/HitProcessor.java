package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_FINALIZE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PENDING_MULTI;
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

        if (game.getSplitActive()) {
            if (playerCount.getLeft() > BJ_CNT) {
                SplitHandHelper.advanceOrFinalize(ctx, ZERO_MULTI, game.getDoubleDown(), ctx.maxSplits());
            } else if (playerCount.getRight().equals(BJ_CNT)) {
                // 21 — auto-advance; multiplier will be computed from score at finalization
                SplitHandHelper.advanceOrFinalize(ctx, PENDING_MULTI, game.getDoubleDown(), ctx.maxSplits());
            } else {
                boolean canAffordSplit = ctx.walletEntity().getBalance()
                        .compareTo(ctx.walletEntity().getHandBet()) >= 0;
                List<Integer> choices = new java.util.ArrayList<>();
                choices.add(CHOICE_STAND);
                choices.add(CHOICE_HIT);
                choices.add(CHOICE_AUTO_FINALIZE);
                if (canAffordSplit && game.isPair() && !game.getSplitAces()
                        && game.getSplitCount() < ctx.maxSplits()) {
                    choices.add(CHOICE_SPLIT);
                }
                game.setAvailableChoices(choices);
            }
            return ctx;
        }

        if (playerCount.getRight().equals(BJ_CNT)) {
            game.dealerPlayUntilSoft17Public();
            game.setFinalized(true);
            // Natural BJ already handled before this point; player hitting to 21 always wins
            // (even if dealer also reaches 21 via multiple cards — that is not a natural BJ)
            game.setHandMultiplier(DOUBLE_MULTI);
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

        game.setAvailableChoices(new java.util.ArrayList<>(List.of(CHOICE_STAND, CHOICE_HIT, CHOICE_AUTO_FINALIZE)));
        return ctx;
    }
}
