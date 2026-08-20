package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT_DD_ADVANCE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_NOT_BASIC_STRATEGY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

/**
 * Handles CHOICE_DOUBLE_DOWN — deducts the additional bet from the wallet,
 * checks funds, and either executes the double or prompts for confirmation
 * when it deviates from basic strategy.
 *
 * During a split, double-down is always executed immediately (no basic-strategy check).
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
        // During a split currentBet has accumulated all split bets; DD only costs one hand bet.
        BigDecimal additionalBet = game.getSplitActive()
                ? ctx.walletEntity().getHandBet()
                : BigDecimal.valueOf(currentBet.doubleValue());

        if (additionalBet.compareTo(ctx.walletEntity().getBalance()) > 0) {
            game.makeChoice(CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY)
                    .setDoubleDown(false)
                    .setErrCodeList(List.of(ERR_CODE_INSUFFICIENT_FUNDS));
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        if (game.getSplitActive()) {
            // During split: deduct handBet (not currentBet — currentBet has accumulated all split bets).
            BigDecimal splitDdBet = additionalBet;
            Wallet wallet = Wallet.of(ctx.walletEntity());
            wallet.setBalance(wallet.getBalance().subtract(splitDdBet));
            wallet.setSplitBet(wallet.getSplitBet().add(splitDdBet));
            wallet.setCurrentBet(wallet.getCurrentBet().add(splitDdBet));
            game.setWallet(wallet);
            Wallet.map(ctx.walletEntity(), wallet);
            ctx.walletRepo().save(ctx.walletEntity());

            game.playerHit();
            game.setDoubleDown(true);
            // Sync the updated playerCards back into splitHands so the template renders all 3 cards.
            game.getSplitHands().set(game.getActiveSplitHandIndex(), new java.util.ArrayList<>(game.getPlayerCards()));
            game.setAvailableChoices(List.of(CHOICE_SPLIT_DD_ADVANCE));
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        Boolean shouldDoubleDown = ctx.basicStrategy().getDoubleDown(game);

        if (shouldDoubleDown) {
            Wallet wallet = Wallet.of(ctx.walletEntity());
            wallet.doubleBet();
            game.setWallet(wallet);
            Wallet.map(ctx.walletEntity(), wallet);
            ctx.walletRepo().save(ctx.walletEntity());

            game.setFinalized(true).setDoubleDown(true);
            game.playerHit();

            if (game.checkBust(game.getPlayerCards())) {
                game.dealerPlayOneCard();
                game.setHandMultiplier(ZERO_MULTI);
            } else {
                game.dealerPlayUntilSoft17Public();
                Integer result = game.compareHands(game.getDealerCards(), game.getPlayerCards());
                if (result < 0) {
                    game.setHandMultiplier(ZERO_MULTI);
                } else if (result == 0) {
                    game.setHandMultiplier(PUSH_MULTI);
                } else {
                    game.setHandMultiplier(DOUBLE_MULTI);
                }
            }

            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        } else {
            game.makeChoice(CHOICE_DOUBLE_NOT_BASIC_STRATEGY)
                    .setAvailableChoices(List.of(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO))
                    .setDoubleDown(false);
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        }

        return ctx;
    }
}
