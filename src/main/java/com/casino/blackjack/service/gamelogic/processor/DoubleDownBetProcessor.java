package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_NOT_BASIC_STRATEGY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;

/**
 * Handles CHOICE_DOUBLE_DOWN — deducts the additional bet from the wallet,
 * checks funds, and either executes the double or prompts for confirmation
 * when it deviates from basic strategy.
 */
public class DoubleDownBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_DOUBLE_DOWN);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        BigDecimal currentBet = ctx.walletEntity().getCurrentBet();
        BigDecimal additionalBet = BigDecimal.valueOf(currentBet.doubleValue());

        if (additionalBet.compareTo(ctx.walletEntity().getBalance()) > 0) {
            game.makeChoice(CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY)
                    .setDoubleDown(false)
                    .setErrCodeList(List.of(ERR_CODE_INSUFFICIENT_FUNDS));
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        Boolean shouldDoubleDown = ctx.basicStrategy().getDoubleDown(game);

        if (shouldDoubleDown) {
            game.setFinalized(true).setDoubleDown(true);
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));

            Wallet wallet = Wallet.of(ctx.walletEntity());
            wallet.doubleBet();
            game.setWallet(wallet);
            Wallet.map(ctx.walletEntity(), wallet);
            ctx.walletRepo().save(ctx.walletEntity());
        } else {
            game.makeChoice(CHOICE_DOUBLE_NOT_BASIC_STRATEGY)
                    .setAvailableChoices(List.of(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO))
                    .setDoubleDown(false);
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        }

        return ctx;
    }
}
