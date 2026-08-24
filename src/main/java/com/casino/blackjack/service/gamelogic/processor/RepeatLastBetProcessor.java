package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_REPEAT_LAST_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;

/**
 * Handles CHOICE_REPEAT_LAST_BET — restores last main + side bets onto the wallet.
 */
public class RepeatLastBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_REPEAT_LAST_BET);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        BigDecimal lastBet    = nvl(ctx.walletEntity().getLastBet());
        BigDecimal lastPpBet  = nvl(ctx.walletEntity().getLastPpBet());
        BigDecimal lastT3Bet  = nvl(ctx.walletEntity().getLastT3Bet());
        BigDecimal lastDppBet = nvl(ctx.walletEntity().getLastDppBet());

        if (lastBet.compareTo(BigDecimal.ZERO) == 0) {
            return ctx;
        }

        BigDecimal totalNeeded = lastBet.add(lastPpBet).add(lastT3Bet).add(lastDppBet);
        if (ctx.walletEntity().getBalance().compareTo(totalNeeded) < 0) {
            game.addErr(ERR_CODE_INSUFFICIENT_FUNDS);
            return ctx;
        }

        Wallet wallet = Wallet.of(ctx.walletEntity());
        // Add lastBet on top of any chips already staged (additive, not replace)
        wallet.setBalance(wallet.getBalance().subtract(lastBet));
        wallet.setCurrentBet(wallet.getCurrentBet().add(lastBet));
        wallet.setHandBet(wallet.getHandBet().add(lastBet));

        if (lastPpBet.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setBalance(wallet.getBalance().subtract(lastPpBet));
            wallet.setPerfectPairsBet(nvl(wallet.getPerfectPairsBet()).add(lastPpBet));
        }
        if (lastT3Bet.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setBalance(wallet.getBalance().subtract(lastT3Bet));
            wallet.setTwentyOneThreeBet(nvl(wallet.getTwentyOneThreeBet()).add(lastT3Bet));
        }
        if (lastDppBet.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setBalance(wallet.getBalance().subtract(lastDppBet));
            wallet.setDealerPerfectPairsBet(nvl(wallet.getDealerPerfectPairsBet()).add(lastDppBet));
        }

        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL))
                .setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        if (ctx.walletRepo() != null) ctx.walletRepo().save(ctx.walletEntity());
        if (ctx.lastGameRepo() != null) ctx.lastGameRepo().save(GameEntity.map(ctx.gameEntity(), game, ctx.om()));

        return ctx;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
