package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.util.ArrayList;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DOUBLE_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.INITIAL_DEALT_CARD_COUNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PUSH_MULTI;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

public class DoubleDownConfirmProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Integer last = ctx.game().getLastTakenChoicePublic();
        return last.equals(CHOICE_DOUBLE_DOWN_YES) || last.equals(CHOICE_DOUBLE_DOWN_NO);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();
        Integer last = game.getLastTakenChoicePublic();

        if (last.equals(CHOICE_DOUBLE_DOWN_YES)) {
            // Double the wallet bet
            Wallet wallet = Wallet.of(ctx.walletEntity());
            wallet.doubleBet();
            wallet.setLastBet(wallet.getHandBet());
            game.setWallet(wallet);
            Wallet.map(ctx.walletEntity(), wallet);
            ctx.walletRepo().save(ctx.walletEntity());

            game.setFinalized(true);
            game.setDoubleDown(true);
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
            return ctx;
        }

        // CHOICE_DOUBLE_DOWN_NO: cancel confirm, restore normal play choices
        game.setFinalized(false);
        List<Integer> choices = new ArrayList<>();
        if (game.getDealerCards().size() == INITIAL_DEALT_CARD_COUNT) {
            choices.add(CHOICE_SURRENDER);
        }
        choices.add(CHOICE_STAND);
        choices.add(CHOICE_HIT);
        choices.add(CHOICE_DOUBLE_DOWN);
        if (game.isPair()) {
            choices.add(CHOICE_SPLIT);
        }
        game.setAvailableChoices(choices);
        return ctx;
    }
}
