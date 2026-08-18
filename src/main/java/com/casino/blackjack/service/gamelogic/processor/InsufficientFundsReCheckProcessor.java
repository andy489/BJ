package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY;

/**
 * After an "insufficient funds" block the player acknowledged the error.
 * Re-checks if balance now covers the pending bet; if still insufficient
 * stays on the error view, otherwise restores the insurance offer choices.
 */
public class InsufficientFundsReCheckProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Integer last = ctx.game().getLastTakenChoicePublic();
        return last.equals(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)
                || last.equals(CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        BigDecimal currentBet = ctx.walletEntity().getCurrentBet();
        BigDecimal balance = ctx.walletEntity().getBalance();

        BigDecimal additionalBet;
        if (game.getLastTakenChoicePublic().equals(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)) {
            additionalBet = BigDecimal.valueOf(currentBet.doubleValue())
                    .divide(BigDecimal.valueOf(2), new MathContext(3));
        } else {
            additionalBet = BigDecimal.valueOf(currentBet.doubleValue());
        }

        if (additionalBet.compareTo(balance) > 0) {
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        game.setAvailableChoices(List.of(CHOICE_INSURANCE_NO, CHOICE_INSURANCE_YES));
        ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        return ctx;
    }
}
