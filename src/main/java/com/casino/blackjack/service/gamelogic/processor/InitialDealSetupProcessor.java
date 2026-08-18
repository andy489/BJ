package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.INITIAL_DEALT_CARD_COUNT;

public class InitialDealSetupProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return true;
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        if (game.getDealerCards().size() == INITIAL_DEALT_CARD_COUNT) {
            if (game.getDealerCards().get(0).getRank().equals(ACE_RANK)) {
                game.setAvailableChoices(java.util.List.of(CHOICE_INSURANCE_YES, CHOICE_INSURANCE_NO));
                return ctx;
            } else {
                game.getAvailableChoices().add(CHOICE_SURRENDER);
            }
        }

        game.getAvailableChoices().addAll(java.util.List.of(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN));

        if (game.isPair()) {
            game.getAvailableChoices().add(CHOICE_SPLIT);
        }

        return ctx;
    }
}
