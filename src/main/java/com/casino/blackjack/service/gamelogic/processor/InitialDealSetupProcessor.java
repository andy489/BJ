package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_PLAY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;

public class InitialDealSetupProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return true;
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        if (game.getDealerCards().size() == 1) {
            if (game.getDealerCards().get(0).getRank().equals(ACE_RANK)) {
                game.setAvailableChoices(java.util.List.of(CHOICE_INSURANCE_YES, CHOICE_INSURANCE_NO));
                return ctx;
            } else {
                game.getAvailableChoices().add(CHOICE_SURRENDER);
            }
        }

        game.getAvailableChoices().addAll(java.util.List.of(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN));

        if (game.getDealerCards().size() == 1) {
            game.getAvailableChoices().add(CHOICE_AUTO_PLAY);
        }

        if (game.isPair() && ctx.walletEntity().getBalance().compareTo(ctx.walletEntity().getHandBet()) >= 0) {
            game.getAvailableChoices().add(CHOICE_SPLIT);
        }

        return ctx;
    }
}
