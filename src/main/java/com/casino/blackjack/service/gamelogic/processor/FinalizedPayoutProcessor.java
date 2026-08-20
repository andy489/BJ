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
 *
 * For split hands, each split hand (including the main hand stored at splitHands[0])
 * pays out handBet * its multiplier. The total is accumulated into lastWin.
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

        BigDecimal totalBetAmount;

        if (game.getSplitActive() == null || !game.getSplitActive()) {
            // Normal (non-split) or split already resolved
            if (game.getSplitHands() != null && !game.getSplitHands().isEmpty()) {
                totalBetAmount = paySplitHands(ctx, game);
            } else {
                totalBetAmount = ctx.walletEntity().payBet(
                        game.getHandMultiplier(), game.getInsuranceMultiplier());
            }
        } else {
            totalBetAmount = ctx.walletEntity().payBet(
                    game.getHandMultiplier(), game.getInsuranceMultiplier());
        }

        ctx.walletRepo().save(ctx.walletEntity());

        BetHistoryEntity betHistory = new BetHistoryEntity()
                .setTotalBetAmount(totalBetAmount)
                .setReturnAmount(ctx.walletEntity().getLastWin())
                .setPlayedGame(playedGame)
                .setDoubleDown(game.getDoubleDown())
                .setUser(playedGame.getOwner());
        ctx.betHistoryService().save(betHistory);

        Game result = Game.of(gameEntity, ctx.om())
                .setAvailableChoices(new java.util.ArrayList<>(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL)))
                .setWallet(Wallet.of(ctx.walletEntity()));

        return new GameContext(result, gameEntity, ctx.walletEntity(),
                ctx.lastGameRepo(), ctx.pastGameRepo(), ctx.walletRepo(),
                ctx.betHistoryService(), ctx.basicStrategy(), ctx.clock(), ctx.om(), ctx.maxSplits());
    }

    /**
     * Pay out all split hands. splitHands[0] = main hand, splitHands[1+] = split hands.
     * Each hand pays handBet * multiplier. Returns total amount wagered.
     */
    private BigDecimal paySplitHands(GameContext ctx, Game game) {
        BigDecimal handBet = ctx.walletEntity().getHandBet();
        List<Double> multipliers = game.getSplitHandMultipliers();

        BigDecimal totalWin = BigDecimal.ZERO;
        BigDecimal totalBet = BigDecimal.ZERO;

        for (int i = 0; i < multipliers.size(); i++) {
            boolean doubled = game.getSplitDoubleDownFlags().get(i);
            BigDecimal betForHand = doubled ? handBet.multiply(BigDecimal.TWO) : handBet;
            totalBet = totalBet.add(betForHand);
            totalWin = totalWin.add(betForHand.multiply(BigDecimal.valueOf(multipliers.get(i))));
        }

        // Also add insurance if applicable
        BigDecimal insuranceBet = ctx.walletEntity().getInsuranceBet();
        BigDecimal insuranceWin = insuranceBet
                .multiply(BigDecimal.valueOf(game.getInsuranceMultiplier()));
        totalWin = totalWin.add(insuranceWin);
        BigDecimal totalCost = totalBet.add(insuranceBet);

        ctx.walletEntity().setLastBet(ctx.walletEntity().getCurrentBet());
        ctx.walletEntity().setLastWin(totalWin.max(BigDecimal.ZERO));
        ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(totalWin));
        ctx.walletEntity().setCurrentBet(BigDecimal.ZERO);
        ctx.walletEntity().setHandBet(BigDecimal.ZERO);
        ctx.walletEntity().setDoubleBet(BigDecimal.ZERO);
        ctx.walletEntity().setInsuranceBet(BigDecimal.ZERO);
        ctx.walletEntity().setSplitBet(BigDecimal.ZERO);

        return totalBet.add(insuranceBet);
    }
}
