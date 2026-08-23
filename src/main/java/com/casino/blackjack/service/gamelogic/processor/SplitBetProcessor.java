package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.BJ_CNT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_FINALIZE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PENDING_MULTI;

/**
 * Handles CHOICE_SPLIT — deducts the additional bet (equal to handBet) from the wallet,
 * initialises the split state in the Game DTO, and sets available choices for
 * the first split hand.
 *
 * Split Aces: only CHOICE_STAND offered (one card already dealt, no further action).
 * All other splits: full choices (hit/stand/double, re-split if pair).
 */
public class SplitBetProcessor implements GameStateProcessor {

    private final int maxSplits;

    public SplitBetProcessor(int maxSplits) {
        this.maxSplits = maxSplits;
    }

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.game().getLastTakenChoicePublic().equals(CHOICE_SPLIT);
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        // Enforce max-splits limit
        if (game.getSplitCount() >= maxSplits) {
            // Should not happen if choices are set correctly, but guard anyway
            game.getAvailableChoices().remove(CHOICE_SPLIT);
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        BigDecimal additionalBet = ctx.walletEntity().getHandBet();

        if (additionalBet.compareTo(ctx.walletEntity().getBalance()) > 0) {
            game.setErrCodeList(List.of(ERR_CODE_INSUFFICIENT_FUNDS));
            ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
            return ctx;
        }

        // Deduct the additional bet and track it in splitBet
        Wallet wallet = Wallet.of(ctx.walletEntity());
        wallet.setBalance(wallet.getBalance().subtract(additionalBet));
        wallet.setSplitBet(wallet.getSplitBet().add(additionalBet));
        wallet.setCurrentBet(wallet.getCurrentBet().add(additionalBet));
        game.setWallet(wallet);
        Wallet.map(ctx.walletEntity(), wallet);
        ctx.walletRepo().save(ctx.walletEntity());

        boolean isAces = game.getPlayerCards().get(0).getRank().equals(ACE_RANK);

        game.initSplit(isAces);

        // Can afford a re-split only if remaining balance >= handBet
        boolean canAffordSplit = wallet.getBalance().compareTo(additionalBet) >= 0;

        // If the first active hand is already 21 after the deal, auto-advance
        int activeScore = game.getCount(game.getActiveHandCards()).getRight();
        if (!isAces && activeScore == BJ_CNT) {
            SplitHandHelper.advanceOrFinalize(ctx, PENDING_MULTI, false, maxSplits);
        } else {
            setChoicesForActiveHand(game, isAces, maxSplits, canAffordSplit);
        }

        ctx.lastGameRepo().save(GameEntity.map(gameEntity, game, ctx.om()));
        return ctx;
    }

    static void setChoicesForActiveHand(Game game, boolean splitAces) {
        setChoicesForActiveHand(game, splitAces, Integer.MAX_VALUE, true);
    }

    static void setChoicesForActiveHand(Game game, boolean splitAces, int maxSplits) {
        setChoicesForActiveHand(game, splitAces, maxSplits, true);
    }

    static void setChoicesForActiveHand(Game game, boolean splitAces, int maxSplits, boolean canAffordSplit) {
        if (splitAces) {
            game.setAvailableChoices(List.of(CHOICE_STAND));
        } else {
            List<Integer> choices = new java.util.ArrayList<>();
            choices.add(CHOICE_STAND);
            choices.add(CHOICE_HIT);
            choices.add(CHOICE_DOUBLE_DOWN);
            choices.add(CHOICE_AUTO_FINALIZE);
            if (canAffordSplit && game.isPair() && game.getSplitCount() < maxSplits) {
                choices.add(CHOICE_SPLIT);
            }
            game.setAvailableChoices(choices);
        }
    }
}
