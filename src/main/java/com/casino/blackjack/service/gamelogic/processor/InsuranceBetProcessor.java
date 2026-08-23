package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;

/**
 * Handles CHOICE_INSURANCE_YES — deducts the insurance bet (half the hand bet)
 * from the wallet, or flags insufficient funds.
 */
public class InsuranceBetProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_INSURANCE_YES);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        if (Boolean.TRUE.equals(game.getInsurance())) {
            return ctx;
        }

        BigDecimal currentBet = ctx.walletEntity().getCurrentBet();
        BigDecimal halfBet = BigDecimal.valueOf(currentBet.doubleValue())
                .divide(BigDecimal.valueOf(2), new MathContext(3));

        if (halfBet.compareTo(ctx.walletEntity().getBalance()) > 0) {
            game.makeChoice(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)
                    .setInsurance(false)
                    .setAvailableChoices(List.of(CHOICE_INSURANCE_NO))
                    .setErrCodeList(List.of(ERR_CODE_INSUFFICIENT_FUNDS));
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        game.setInsurance(true);
        Wallet wallet = Wallet.of(ctx.walletEntity());
        wallet.placeInsurance(halfBet);
        game.setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        ctx.walletRepo().save(ctx.walletEntity());
        ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));

        return ctx;
    }
}
