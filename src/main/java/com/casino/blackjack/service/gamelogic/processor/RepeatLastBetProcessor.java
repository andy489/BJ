package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_REPEAT_LAST_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_LAST_BET;

/**
 * Handles CHOICE_REPEAT_LAST_BET — places the last recorded bet on the wallet.
 */
public class RepeatLastBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_REPEAT_LAST_BET);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        BigDecimal lastBet = ctx.walletEntity().getLastBet();

        if (lastBet.compareTo(BigDecimal.ZERO) == 0) {
            return ctx; // game.addErr(NO_LAST_BET) was already set by GameService.repeatLastBet()
        }

        if (ctx.walletEntity().getBalance().compareTo(lastBet) < 0) {
            game.addErr(ERR_CODE_INSUFFICIENT_FUNDS);
            return ctx;
        }

        Wallet wallet = Wallet.of(ctx.walletEntity());
        wallet.placeHandBet(lastBet);
        game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL))
                .setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        ctx.walletRepo().save(ctx.walletEntity());

        return ctx;
    }
}
