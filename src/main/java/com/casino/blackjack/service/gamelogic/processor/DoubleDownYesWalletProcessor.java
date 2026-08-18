package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;

/**
 * Handles CHOICE_DOUBLE_DOWN_YES from the "not basic strategy" confirmation modal.
 * Doubles the wallet bet and persists.
 */
public class DoubleDownYesWalletProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_DOUBLE_DOWN_YES);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        game.setDoubleDown(true);

        Wallet wallet = Wallet.of(ctx.walletEntity());
        wallet.doubleBet();
        wallet.setLastBet(wallet.getHandBet());

        game.setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        ctx.walletRepo().save(ctx.walletEntity());
        ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));

        return ctx;
    }
}
