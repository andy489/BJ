package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CLEAR_LAST_BET;

/**
 * Handles CHOICE_CLEAR_LAST_BET — returns the currently staged bet amounts to the balance.
 * Refunds handBet + perfectPairsBet + twentyOneThreeBet + dealerPerfectPairsBet and
 * zeroes all four fields plus currentBet. Does NOT touch lastBet.
 */
public class ClearLastBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_CLEAR_LAST_BET);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Wallet wallet = Wallet.of(ctx.walletEntity());

        BigDecimal stagedBet = nvl(wallet.getHandBet())
                .add(nvl(wallet.getPerfectPairsBet()))
                .add(nvl(wallet.getTwentyOneThreeBet()))
                .add(nvl(wallet.getDealerPerfectPairsBet()));

        wallet.deposit(stagedBet);
        wallet.setHandBet(BigDecimal.ZERO);
        wallet.setPerfectPairsBet(BigDecimal.ZERO);
        wallet.setTwentyOneThreeBet(BigDecimal.ZERO);
        wallet.setDealerPerfectPairsBet(BigDecimal.ZERO);
        wallet.setCurrentBet(BigDecimal.ZERO);

        ctx.game().setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        ctx.walletRepo().save(ctx.walletEntity());

        return ctx;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
