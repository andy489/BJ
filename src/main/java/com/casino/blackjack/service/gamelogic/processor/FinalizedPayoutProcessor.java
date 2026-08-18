package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.PlayedGameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;

/**
 * Handles a finalized game — moves it from last_games to played_games,
 * applies the payout to the wallet, and saves bet history.
 */
public class FinalizedPayoutProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.gameEntity() != null && ctx.gameEntity().getFinalized();
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        ctx.lastGameRepo().delete(gameEntity);
        PlayedGameEntity playedGame = PlayedGameEntity.of(gameEntity)
                .setFinalizedTime(ctx.clock().getNow());
        ctx.pastGameRepo().save(playedGame);

        BigDecimal totalBetAmount = ctx.walletEntity().payBet(
                game.getHandMultiplier(), game.getInsuranceMultiplier());
        ctx.walletRepo().save(ctx.walletEntity());

        BetHistoryEntity betHistory = new BetHistoryEntity()
                .setTotalBetAmount(totalBetAmount)
                .setReturnAmount(ctx.walletEntity().getLastWin())
                .setPlayedGame(playedGame)
                .setDoubleDown(game.getDoubleDown())
                .setUser(playedGame.getOwner());
        ctx.betHistoryService().save(betHistory);

        Game result = Game.of(gameEntity, ctx.om())
                .addAvailableChoice(CHOICE_CHIP_OPERATIONS)
                .setWallet(Wallet.of(ctx.walletEntity()));

        return new GameContext(result, gameEntity, ctx.walletEntity(),
                ctx.lastGameRepo(), ctx.pastGameRepo(), ctx.walletRepo(),
                ctx.betHistoryService(), ctx.basicStrategy(), ctx.clock(), ctx.om());
    }
}
