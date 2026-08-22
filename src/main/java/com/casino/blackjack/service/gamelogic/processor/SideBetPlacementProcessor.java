package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_21_3;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_PERFECT_PAIRS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MAX;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MIN;

/**
 * Handles CHOICE_PLACE_PERFECT_PAIRS and CHOICE_PLACE_21_3.
 * Only fires before the deal (dealt == false).
 * Deducts the side bet from the balance and stores it on the wallet.
 * A second POST with the same choice toggles the bet off (remove and refund).
 */
public class SideBetPlacementProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        if (ctx.gameEntity() == null || Boolean.TRUE.equals(ctx.gameEntity().getFinalized())) {
            return false;
        }
        if (Boolean.TRUE.equals(ctx.gameEntity().getFinalized())) return false;
        int choice = ctx.game().getLastTakenChoicePublic();
        return choice == CHOICE_PLACE_PERFECT_PAIRS || choice == CHOICE_PLACE_21_3;
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();
        int choice = game.getLastTakenChoicePublic();

        boolean isPP = choice == CHOICE_PLACE_PERFECT_PAIRS;
        BigDecimal current = isPP
                ? ctx.walletEntity().getPerfectPairsBet()
                : ctx.walletEntity().getTwentyOneThreeBet();

        if (current != null && current.compareTo(BigDecimal.ZERO) > 0) {
            // Toggle off — refund
            ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(current));
            if (isPP) {
                ctx.walletEntity().setPerfectPairsBet(BigDecimal.ZERO);
            } else {
                ctx.walletEntity().setTwentyOneThreeBet(BigDecimal.ZERO);
            }
            ctx.walletRepo().save(ctx.walletEntity());
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        // Place minimum side bet
        BigDecimal bet = SIDE_BET_MIN;
        if (bet.compareTo(ctx.walletEntity().getBalance()) > 0) {
            game.addErr(ERR_CODE_INSUFFICIENT_FUNDS);
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().subtract(bet));
        if (isPP) {
            ctx.walletEntity().setPerfectPairsBet(bet);
        } else {
            ctx.walletEntity().setTwentyOneThreeBet(bet);
        }
        ctx.walletRepo().save(ctx.walletEntity());
        ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        return ctx;
    }
}
