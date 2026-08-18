package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.util.Collections;

/**
 * If the game has errors, pass them through to the view and clear them in the DB
 * so they don't persist past the current render.
 */
public class ErrorPassthroughProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return !ctx.game().getErrCodeList().isEmpty();
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        game.setWallet(Wallet.of(ctx.walletEntity()));
        Game cleared = new Game(game).setErrCodeList(Collections.emptyList());
        ctx.lastGameRepo().save(
                com.casino.blackjack.model.entity.GameEntity.map(ctx.gameEntity(), cleared, ctx.om()));

        // Return original game (with errors) so the view can display them
        return ctx;
    }
}
