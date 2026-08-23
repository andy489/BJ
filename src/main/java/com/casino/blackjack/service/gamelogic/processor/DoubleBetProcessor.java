package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.MAX_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MAX;

/**
 * Handles CHOICE_DOUBLE_BET — doubles every currently-placed bet (main + side bets),
 * each capped at its own maximum. Requires sufficient balance for the added amounts.
 * Shows ERR_CODE_INSUFFICIENT_FUNDS modal if the player cannot afford the double.
 */
public class DoubleBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_DOUBLE_BET);
    }

    @Override
    public GameContext process(GameContext ctx) {
        var we = ctx.walletEntity();
        var game = ctx.game();

        BigDecimal handBet = nvl(we.getHandBet());
        BigDecimal ppBet   = nvl(we.getPerfectPairsBet());
        BigDecimal t3Bet   = nvl(we.getTwentyOneThreeBet());
        BigDecimal dppBet  = nvl(we.getDealerPerfectPairsBet());

        // Nothing placed yet — nothing to double
        if (handBet.compareTo(BigDecimal.ZERO) == 0
                && ppBet.compareTo(BigDecimal.ZERO) == 0
                && t3Bet.compareTo(BigDecimal.ZERO) == 0
                && dppBet.compareTo(BigDecimal.ZERO) == 0) {
            return ctx;
        }

        // Compute how much each bet can grow (capped at its max)
        BigDecimal addHand = handBet.min(MAX_BET.subtract(handBet)).max(BigDecimal.ZERO);
        BigDecimal addPp   = ppBet.compareTo(BigDecimal.ZERO) > 0
                ? ppBet.min(SIDE_BET_MAX.subtract(ppBet)).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        BigDecimal addT3   = t3Bet.compareTo(BigDecimal.ZERO) > 0
                ? t3Bet.min(SIDE_BET_MAX.subtract(t3Bet)).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        BigDecimal addDpp  = dppBet.compareTo(BigDecimal.ZERO) > 0
                ? dppBet.min(SIDE_BET_MAX.subtract(dppBet)).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        BigDecimal totalAdded = addHand.add(addPp).add(addT3).add(addDpp);

        if (totalAdded.compareTo(BigDecimal.ZERO) == 0) {
            return ctx; // all bets already at max
        }

        if (we.getBalance().compareTo(totalAdded) < 0) {
            game.addErr(ERR_CODE_INSUFFICIENT_FUNDS);
            if (ctx.lastGameRepo() != null) ctx.lastGameRepo().save(com.casino.blackjack.model.entity.GameEntity.map(ctx.gameEntity(), game, ctx.om()));
            return ctx;
        }

        Wallet wallet = Wallet.of(we);
        wallet.setBalance(wallet.getBalance().subtract(totalAdded));
        wallet.setHandBet(handBet.add(addHand));
        wallet.setCurrentBet(wallet.getCurrentBet().add(addHand).add(addPp).add(addT3).add(addDpp));
        if (addPp.compareTo(BigDecimal.ZERO) > 0)   wallet.setPerfectPairsBet(ppBet.add(addPp));
        if (addT3.compareTo(BigDecimal.ZERO) > 0)   wallet.setTwentyOneThreeBet(t3Bet.add(addT3));
        if (addDpp.compareTo(BigDecimal.ZERO) > 0)  wallet.setDealerPerfectPairsBet(dppBet.add(addDpp));

        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL))
                .setWallet(wallet);
        Wallet.map(we, wallet);
        if (ctx.walletRepo() != null) ctx.walletRepo().save(we);
        if (ctx.lastGameRepo() != null) ctx.lastGameRepo().save(com.casino.blackjack.model.entity.GameEntity.map(ctx.gameEntity(), game, ctx.om()));

        return ctx;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
