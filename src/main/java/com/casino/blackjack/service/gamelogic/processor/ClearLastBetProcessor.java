package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CLEAR_LAST_BET;

/**
 * Handles CHOICE_CLEAR_LAST_BET — returns the current bet to the balance.
 */
public class ClearLastBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_CLEAR_LAST_BET);
    }

    @Override
    public GameContext process(GameContext ctx) {
        BigDecimal lastBet = ctx.walletEntity().getLastBet();

        Wallet wallet = Wallet.of(ctx.walletEntity());
        wallet.setLastBet(BigDecimal.ZERO);
        wallet.deposit(lastBet);
        wallet.setCurrentBet(BigDecimal.ZERO);

        ctx.game().setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        ctx.walletRepo().save(ctx.walletEntity());

        return ctx;
    }
}
