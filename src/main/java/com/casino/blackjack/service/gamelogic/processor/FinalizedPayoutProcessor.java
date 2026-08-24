package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.PlayedGameEntity;
import com.casino.blackjack.service.gamelogic.SideBetEvaluator;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;

/**
 * Handles a finalized game — moves it from last_games to played_games,
 * applies the payout to the wallet, and saves bet history.
 *
 * For split hands, each split hand (including the main hand stored at splitHands[0])
 * pays out handBet * its multiplier. The total is accumulated into lastWin.
 *
 * Side bets (Perfect Pairs, 21+3) are settled here regardless of the main hand outcome.
 * They are evaluated on the initial two player cards and dealer up-card stored at deal time.
 */
public class FinalizedPayoutProcessor implements GameStateProcessor {

    @Override
    public boolean canProcess(GameContext ctx) {
        return ctx.gameEntity() != null && ctx.gameEntity().getFinalized();
    }

    @Override
    public GameContext process(GameContext ctx) {
        Game game = ctx.game();
        GameEntity gameEntity = ctx.gameEntity();

        ctx.lastGameRepo().delete(gameEntity);
        PlayedGameEntity playedGame = PlayedGameEntity.of(gameEntity)
                .setFinalizedTime(ctx.clock().getNow());
        ctx.pastGameRepo().save(playedGame);

        BigDecimal totalBetAmount;

        // Capture side bet totals before payBet() zeroes them
        BigDecimal ppBetSnapshot  = nvl(ctx.walletEntity().getPerfectPairsBet());
        BigDecimal t3BetSnapshot  = nvl(ctx.walletEntity().getTwentyOneThreeBet());
        BigDecimal dppBetSnapshot = nvl(ctx.walletEntity().getDealerPerfectPairsBet());

        if (game.getSplitActive() == null || !game.getSplitActive()) {
            if (game.getSplitHands() != null && !game.getSplitHands().isEmpty()) {
                totalBetAmount = paySplitHands(ctx, game);
            } else {
                totalBetAmount = ctx.walletEntity().payBet(
                        game.getHandMultiplier(), game.getInsuranceMultiplier());
            }
        } else {
            totalBetAmount = ctx.walletEntity().payBet(
                    game.getHandMultiplier(), game.getInsuranceMultiplier());
        }

        // Settle side bets (independent of main hand outcome)
        settleSideBets(ctx, gameEntity, ppBetSnapshot, t3BetSnapshot, dppBetSnapshot);

        // Capture main-hand-only return before rolling side-bet returns into lastWin
        ctx.walletEntity().setLastHandWin(nvl(ctx.walletEntity().getLastWin()));

        // Roll side-bet net profits into lastWin so the "Last Win" box is consistent with
        // the breakdown labels (PP/T3/DPP show net profit, hand shows gross return).
        // lastPpWin/T3Win/DppWin already hold net profit (gross return - stake).
        BigDecimal ppNet  = nvl(ctx.walletEntity().getLastPpWin());
        BigDecimal t3Net  = nvl(ctx.walletEntity().getLastT3Win());
        BigDecimal dppNet = nvl(ctx.walletEntity().getLastDppWin());
        BigDecimal combinedLastWin = nvl(ctx.walletEntity().getLastWin())
                .add(ppNet).add(t3Net).add(dppNet);
        ctx.walletEntity().setLastWin(combinedLastWin);

        // lastBet = main hand bet (for Repeat); lastTotalBet = all bets combined (for display)
        BigDecimal lastTotalBet = ctx.walletEntity().getLastBet()
                .add(ppBetSnapshot).add(t3BetSnapshot).add(dppBetSnapshot);
        ctx.walletEntity().setLastTotalBet(lastTotalBet);
        ctx.walletEntity().setLastPpBet(ppBetSnapshot);
        ctx.walletEntity().setLastT3Bet(t3BetSnapshot);
        ctx.walletEntity().setLastDppBet(dppBetSnapshot);

        ctx.walletRepo().save(ctx.walletEntity());

        BetHistoryEntity betHistory = new BetHistoryEntity()
                .setTotalBetAmount(totalBetAmount)
                .setReturnAmount(ctx.walletEntity().getLastWin())
                .setPlayedGame(playedGame)
                .setDoubleDown(game.getDoubleDown())
                .setSplit(game.getSplitHands() != null && !game.getSplitHands().isEmpty())
                .setPpBet(ppBetSnapshot)
                .setT3Bet(t3BetSnapshot)
                .setDppBet(dppBetSnapshot)
                .setPpWin(nvl(ctx.walletEntity().getLastPpWin()))
                .setT3Win(nvl(ctx.walletEntity().getLastT3Win()))
                .setDppWin(nvl(ctx.walletEntity().getLastDppWin()))
                .setUser(playedGame.getOwner());
        ctx.betHistoryService().save(betHistory);

        Game result = game
                .setDealt(false)
                .setFinalized(true)  // keep true so the result overlay renders; entity already deleted from last_games
                .setAvailableChoices(new java.util.ArrayList<>(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL)))
                .setWallet(Wallet.of(ctx.walletEntity()));

        return new GameContext(result, gameEntity, ctx.walletEntity(),
                ctx.lastGameRepo(), ctx.pastGameRepo(), ctx.walletRepo(),
                ctx.betHistoryService(), ctx.basicStrategy(), ctx.clock(), ctx.om(), ctx.maxSplits(), ctx.resultDisplayMs(),
                ctx.paytable());
    }

    private void settleSideBets(GameContext ctx, GameEntity gameEntity,
                                BigDecimal ppBet, BigDecimal t3Bet, BigDecimal dppBet) {
        boolean hasPP  = ppBet  != null && ppBet.compareTo(BigDecimal.ZERO)  > 0;
        boolean hasT3  = t3Bet  != null && t3Bet.compareTo(BigDecimal.ZERO)  > 0;
        boolean hasDPP = dppBet != null && dppBet.compareTo(BigDecimal.ZERO) > 0;

        if (!hasPP && !hasT3 && !hasDPP) {
            ctx.walletEntity().setLastPpWin(BigDecimal.ZERO);
            ctx.walletEntity().setLastT3Win(BigDecimal.ZERO);
            ctx.walletEntity().setLastDppWin(BigDecimal.ZERO);
            return;
        }

        String initialPlayerCardsJson = gameEntity.getInitialPlayerCards();
        String initialDealerUpCardJson = gameEntity.getInitialDealerUpCard();
        String initialDealerCardsJson  = gameEntity.getInitialDealerCards();

        if (initialPlayerCardsJson == null || initialDealerUpCardJson == null) {
            // No initial cards stored — forfeit side bets silently
            ctx.walletEntity().setPerfectPairsBet(BigDecimal.ZERO);
            ctx.walletEntity().setTwentyOneThreeBet(BigDecimal.ZERO);
            ctx.walletEntity().setDealerPerfectPairsBet(BigDecimal.ZERO);
            return;
        }

        try {
            List<Card> initialPlayerCards = ctx.om().readValue(initialPlayerCardsJson, new TypeReference<>() {});
            Card dealerUpCard = ctx.om().readValue(initialDealerUpCardJson, Card.class);

            if (initialPlayerCards.size() < 2) return;
            Card p0 = initialPlayerCards.get(0);
            Card p1 = initialPlayerCards.get(1);

            if (hasPP) {
                double multi = SideBetEvaluator.evalPerfectPairs(p0, p1, ctx.paytable());
                BigDecimal ppReturn = ppBet.multiply(BigDecimal.valueOf(multi));
                BigDecimal ppNet = ppReturn.subtract(ppBet).max(BigDecimal.ZERO);
                ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(ppReturn));
                ctx.walletEntity().setLastPpWin(ppNet);
                ctx.walletEntity().setPerfectPairsBet(BigDecimal.ZERO);
            } else {
                ctx.walletEntity().setLastPpWin(BigDecimal.ZERO);
            }

            if (hasT3) {
                double multi = SideBetEvaluator.eval21_3(p0, p1, dealerUpCard, ctx.paytable());
                BigDecimal t3Return = t3Bet.multiply(BigDecimal.valueOf(multi));
                BigDecimal t3Net = t3Return.subtract(t3Bet).max(BigDecimal.ZERO);
                ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(t3Return));
                ctx.walletEntity().setLastT3Win(t3Net);
                ctx.walletEntity().setTwentyOneThreeBet(BigDecimal.ZERO);
            } else {
                ctx.walletEntity().setLastT3Win(BigDecimal.ZERO);
            }

            if (hasDPP && initialDealerCardsJson != null) {
                List<Card> initialDealerCards = ctx.om().readValue(initialDealerCardsJson, new TypeReference<>() {});
                if (initialDealerCards.size() >= 2) {
                    double multi = SideBetEvaluator.evalPerfectPairs(initialDealerCards.get(0), initialDealerCards.get(1), ctx.paytable());
                    BigDecimal dppReturn = dppBet.multiply(BigDecimal.valueOf(multi));
                    BigDecimal dppNet = dppReturn.subtract(dppBet).max(BigDecimal.ZERO);
                    ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(dppReturn));
                    ctx.walletEntity().setLastDppWin(dppNet);
                }
                ctx.walletEntity().setDealerPerfectPairsBet(BigDecimal.ZERO);
            } else {
                ctx.walletEntity().setLastDppWin(BigDecimal.ZERO);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pay out all split hands. splitHands[0] = main hand, splitHands[1+] = split hands.
     * Each hand pays handBet * multiplier. Returns total amount wagered.
     */
    private BigDecimal paySplitHands(GameContext ctx, Game game) {
        BigDecimal handBet = ctx.walletEntity().getHandBet();
        List<Double> multipliers = game.getSplitHandMultipliers();

        BigDecimal totalWin = BigDecimal.ZERO;
        BigDecimal totalBet = BigDecimal.ZERO;

        for (int i = 0; i < multipliers.size(); i++) {
            boolean doubled = game.getSplitDoubleDownFlags().get(i);
            BigDecimal betForHand = doubled ? handBet.multiply(BigDecimal.TWO) : handBet;
            totalBet = totalBet.add(betForHand);
            totalWin = totalWin.add(betForHand.multiply(BigDecimal.valueOf(multipliers.get(i))));
        }

        BigDecimal insuranceBet = ctx.walletEntity().getInsuranceBet();
        BigDecimal insuranceWin = insuranceBet
                .multiply(BigDecimal.valueOf(game.getInsuranceMultiplier()));
        totalWin = totalWin.add(insuranceWin);

        // Net profit = total returned − total staked (consistent with Wallet.payBet())
        BigDecimal totalStaked = totalBet.add(insuranceBet);
        BigDecimal netProfit = totalWin.subtract(totalStaked);

        ctx.walletEntity().setLastBet(handBet);
        ctx.walletEntity().setLastWin(netProfit.max(BigDecimal.ZERO));
        ctx.walletEntity().setBalance(ctx.walletEntity().getBalance().add(totalWin));
        ctx.walletEntity().setCurrentBet(BigDecimal.ZERO);
        ctx.walletEntity().setHandBet(BigDecimal.ZERO);
        ctx.walletEntity().setDoubleBet(BigDecimal.ZERO);
        ctx.walletEntity().setInsuranceBet(BigDecimal.ZERO);
        ctx.walletEntity().setSplitBet(BigDecimal.ZERO);
        ctx.walletEntity().setPerfectPairsBet(BigDecimal.ZERO);
        ctx.walletEntity().setTwentyOneThreeBet(BigDecimal.ZERO);
        ctx.walletEntity().setDealerPerfectPairsBet(BigDecimal.ZERO);

        return totalBet.add(insuranceBet);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
