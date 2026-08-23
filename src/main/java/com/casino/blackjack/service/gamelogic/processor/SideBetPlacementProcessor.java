package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_21_3;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_PERFECT_PAIRS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_DEALER_PP;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MAX;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MIN;

/**
 * Handles CHOICE_PLACE_PERFECT_PAIRS and CHOICE_PLACE_21_3.
 * Only fires before the deal (dealt == false).
 *
 * Behaviour:
 *   - If betStr is null/blank or zero: toggle — remove existing bet and refund.
 *   - Otherwise: add betStr to the existing side bet (chip-style increment).
 *     Clamps to SIDE_BET_MAX; rejects if balance is insufficient.
 */
public class SideBetPlacementProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        if (ctx.gameEntity() == null || Boolean.TRUE.equals(ctx.gameEntity().getFinalized())) {
            return false;
        }
        if (ctx.gameEntity().getInitialPlayerCards() != null) {
            return false;
        }
        int choice = ctx.game().getLastTakenChoicePublic();
        return choice == CHOICE_PLACE_PERFECT_PAIRS || choice == CHOICE_PLACE_21_3 || choice == CHOICE_PLACE_DEALER_PP;
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();
        int choice = game.getLastTakenChoicePublic();
        boolean isPP = choice == CHOICE_PLACE_PERFECT_PAIRS;
        boolean isDPP = choice == CHOICE_PLACE_DEALER_PP;

        BigDecimal current = isPP
                ? ctx.walletEntity().getPerfectPairsBet()
                : isDPP
                    ? ctx.walletEntity().getDealerPerfectPairsBet()
                    : ctx.walletEntity().getTwentyOneThreeBet();
        if (current == null) current = BigDecimal.ZERO;

        String betStr = game.getSideBetAmountStr();
        boolean isToggleOff = betStr == null || betStr.isBlank() || betStr.equals("0") || betStr.equals("0.00");

        if (isToggleOff) {
            // Refund existing bet
            if (current.compareTo(BigDecimal.ZERO) > 0) {
                ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(current));
                if (isPP) {
                    ctx.walletEntity().setPerfectPairsBet(BigDecimal.ZERO);
                } else if (isDPP) {
                    ctx.walletEntity().setDealerPerfectPairsBet(BigDecimal.ZERO);
                } else {
                    ctx.walletEntity().setTwentyOneThreeBet(BigDecimal.ZERO);
                }
                ctx.walletRepo().save(ctx.walletEntity());
            }
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        // Parse and validate the amount
        BigDecimal amount;
        try {
            amount = new BigDecimal(betStr);
        } catch (NumberFormatException e) {
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        // Clamp total to SIDE_BET_MAX
        BigDecimal newTotal = current.add(amount);
        if (newTotal.compareTo(SIDE_BET_MAX) > 0) {
            amount = SIDE_BET_MAX.subtract(current);
            newTotal = SIDE_BET_MAX;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            // Already at max, nothing to add
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        if (amount.compareTo(ctx.walletEntity().getBalance()) > 0) {
            game.addErr(ERR_CODE_INSUFFICIENT_FUNDS);
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().subtract(amount));
        if (isPP) {
            ctx.walletEntity().setPerfectPairsBet(newTotal);
        } else if (isDPP) {
            ctx.walletEntity().setDealerPerfectPairsBet(newTotal);
        } else {
            ctx.walletEntity().setTwentyOneThreeBet(newTotal);
        }
        ctx.walletRepo().save(ctx.walletEntity());
        ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        return ctx;
    }
}
