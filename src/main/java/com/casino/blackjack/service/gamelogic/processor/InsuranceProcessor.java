package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.service.gamelogic.dto.Game;

import java.util.ArrayList;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_FINALIZE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_PLAY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ZERO_MULTI;

public class InsuranceProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        Integer last = ctx.game().getLastTakenChoicePublic();
        return last.equals(CHOICE_INSURANCE_YES) || last.equals(CHOICE_INSURANCE_NO);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();

        if (game.getLastTakenChoicePublic().equals(CHOICE_INSURANCE_YES)) {
            game.setInsurance(true);
        }

        if (game.isDealerHiddenCardBJ()) {
            game.setFinalized(true);
            game.setHandMultiplier(ZERO_MULTI);
            game.getDealerCards().add(game.getDealerSecondCard());
            game.setDealerSecondCard(null);

            if (game.getInsurance()) {
                game.setInsuranceMultiplier(ctx.paytable().insuranceMulti());
            }

            game.setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            return ctx;
        }

        List<Integer> choices = new ArrayList<>(List.of(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN, CHOICE_AUTO_FINALIZE, CHOICE_AUTO_PLAY));
        game.setAvailableChoices(choices);

        if (game.isPair()) {
            game.getAvailableChoices().add(CHOICE_SPLIT);
        }

        return ctx;
    }
}
