package com.casino.blackjack.service.simulation;

import com.casino.blackjack.config.PaytableProperties;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.rng.RngCardSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;
import static com.casino.blackjack.service.simulation.BasicStrategyAdvisor.Action;

@Service
public class SimulationService {

    private final double bjMulti;

    public SimulationService(PaytableProperties paytableProperties) {
        this.bjMulti = paytableProperties.bjMulti();
    }

    public SimulationResult simulate(long n, double bet, int threads, SimulationStrategy strategy) {
        if (!strategy.isImplemented()) {
            throw new UnsupportedOperationException("Strategy not implemented: " + strategy.getLabel());
        }
        long chunkSize = n / threads;
        long remainder = n % threads;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<long[]>> futures = new ArrayList<>(threads);

        long startMs = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            long hands = chunkSize + (t == 0 ? remainder : 0);
            final double betAmt = bet;
            futures.add(pool.submit(() ->
                strategy == SimulationStrategy.BASIC_STRATEGY
                    ? simulateChunkBasic(hands, betAmt)
                    : simulateChunkMirror(hands, betAmt)
            ));
        }

        pool.shutdown();

        double totalWagered  = 0;
        double totalReturned = 0;
        long wins = 0, losses = 0, pushes = 0, blackjacks = 0;

        try {
            for (Future<long[]> f : futures) {
                long[] r = f.get();
                totalWagered  += Double.longBitsToDouble(r[0]);
                totalReturned += Double.longBitsToDouble(r[1]);
                wins       += r[2];
                losses     += r[3];
                pushes     += r[4];
                blackjacks += r[5];
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulation interrupted", e);
        }

        long elapsedMs = System.currentTimeMillis() - startMs;

        return SimulationResult.of(n, totalWagered, totalReturned,
                wins, losses, pushes, blackjacks, strategy.getDescription(), elapsedMs);
    }

    // ── Dealer-mirror chunk ──────────────────────────────────────────────────

    private long[] simulateChunkMirror(long n, double BET) {
        double totalWagered  = 0;
        double totalReturned = 0;
        long wins = 0, losses = 0, pushes = 0, blackjacks = 0;

        for (long i = 0; i < n; i++) {
            Game game = new Game();
            game.setCardSource(new RngCardSource());
            game.deal();

            List<Card> playerCards = game.getPlayerCards();
            List<Card> dealerCards = game.getDealerCards();
            Card dealerHidden = dealerCards.remove(1);

            totalWagered += BET;
            boolean playerBJ = game.checkBJCards(playerCards);

            if (!playerBJ) {
                while (mirrorShouldHit(game.getCount(playerCards))) {
                    playerCards.add(game.getCardSource().next());
                }
            }

            boolean playerBust = isBust(game.getCount(playerCards));
            dealerCards.add(dealerHidden);
            game.dealerPlayUntilSoft17Public();
            boolean dealerBJ   = game.checkBJCards(dealerCards);
            boolean dealerBust = isBust(game.getCount(dealerCards));

            double returned;
            if (playerBust) {
                returned = 0; losses++;
            } else if (playerBJ && !dealerBJ) {
                returned = BET * bjMulti; blackjacks++; wins++;
            } else if (playerBJ && dealerBJ) {
                returned = BET * PUSH_MULTI; pushes++;
            } else if (dealerBust) {
                returned = BET * DOUBLE_MULTI; wins++;
            } else {
                int cmp = game.compareHands(dealerCards, playerCards);
                if (cmp > 0)       { returned = BET * DOUBLE_MULTI; wins++; }
                else if (cmp == 0) { returned = BET * PUSH_MULTI;   pushes++; }
                else               { returned = 0;                   losses++; }
            }
            totalReturned += returned;
        }

        return new long[]{
                Double.doubleToLongBits(totalWagered),
                Double.doubleToLongBits(totalReturned),
                wins, losses, pushes, blackjacks
        };
    }

    // ── Basic-strategy chunk ─────────────────────────────────────────────────

    private long[] simulateChunkBasic(long n, double BET) {
        double totalWagered  = 0;
        double totalReturned = 0;
        long wins = 0, losses = 0, pushes = 0, blackjacks = 0;

        for (long i = 0; i < n; i++) {
            Game game = new Game();
            game.setCardSource(new RngCardSource());
            game.deal();

            List<Card> playerCards = game.getPlayerCards();
            List<Card> dealerCards = game.getDealerCards();
            Card dealerHidden  = dealerCards.remove(1);
            int  dealerUpcard  = BasicStrategyAdvisor.normalizeRank(dealerCards.get(0).getRank());
            boolean playerBJ   = game.checkBJCards(playerCards);

            if (playerBJ) {
                // Natural — no player action needed
                totalWagered += BET;
                dealerCards.add(dealerHidden);
                game.dealerPlayUntilSoft17Public();
                boolean dealerBJ = game.checkBJCards(dealerCards);
                double returned = dealerBJ ? BET * PUSH_MULTI : BET * bjMulti;
                if (!dealerBJ) { blackjacks++; wins++; } else pushes++;
                totalReturned += returned;
                continue;
            }

            // ── Check for split ──
            Action firstAction = BasicStrategyAdvisor.advise(playerCards, dealerUpcard, true);

            if (firstAction == Action.SPLIT) {
                // Two hands, each gets one original card + one new card
                Card splitCard = playerCards.remove(1);
                List<Card> hand1 = new ArrayList<>(playerCards); // one card
                List<Card> hand2 = new ArrayList<>();
                hand2.add(splitCard);
                hand1.add(game.getCardSource().next());
                hand2.add(game.getCardSource().next());

                double w1 = playBasicHand(hand1, dealerUpcard, game) * BET;
                double w2 = playBasicHand(hand2, dealerUpcard, game) * BET;
                totalWagered += w1 + w2;

                dealerCards.add(dealerHidden);
                game.dealerPlayUntilSoft17Public();
                boolean dealerBust = isBust(game.getCount(dealerCards));

                double[] r1r2 = settleTwo(hand1, w1, hand2, w2, dealerCards, dealerBust, game);
                totalReturned += r1r2[0] + r1r2[1];

                if (r1r2[0] > w1)        wins++;   else if (r1r2[0] == w1) pushes++; else losses++;
                if (r1r2[1] > w2)        wins++;   else if (r1r2[1] == w2) pushes++; else losses++;
                continue;
            }

            // ── Double down ──
            double wagered = BET;
            if (firstAction == Action.DOUBLE) {
                wagered = BET * 2;
                playerCards.add(game.getCardSource().next());
                // Stand after double — no further hits
            } else {
                // Hit / stand loop
                while (true) {
                    if (isBust(BasicStrategyAdvisor.computeCount(playerCards))) break;
                    Action act = BasicStrategyAdvisor.advise(playerCards, dealerUpcard, false);
                    if (act == Action.STAND) break;
                    playerCards.add(game.getCardSource().next());
                }
            }

            totalWagered += wagered;

            boolean playerBust = isBust(BasicStrategyAdvisor.computeCount(playerCards));
            dealerCards.add(dealerHidden);
            game.dealerPlayUntilSoft17Public();
            boolean dealerBust = isBust(game.getCount(dealerCards));

            double returned;
            if (playerBust) {
                returned = 0; losses++;
            } else if (dealerBust) {
                returned = wagered * DOUBLE_MULTI; wins++;
            } else {
                int cmp = game.compareHands(dealerCards, playerCards);
                if (cmp > 0)       { returned = wagered * DOUBLE_MULTI; wins++; }
                else if (cmp == 0) { returned = wagered * PUSH_MULTI;   pushes++; }
                else               { returned = 0;                       losses++; }
            }
            totalReturned += returned;
        }

        return new long[]{
                Double.doubleToLongBits(totalWagered),
                Double.doubleToLongBits(totalReturned),
                wins, losses, pushes, blackjacks
        };
    }

    /** Plays a split sub-hand; returns effective wager (BET or BET*2 if doubled). */
    private double playBasicHand(List<Card> hand, int dealerUpcard, Game game) {
        double wagered = 0; // will be set by caller — just play the hand
        Action act = BasicStrategyAdvisor.advise(hand, dealerUpcard, true);
        if (act == Action.DOUBLE) {
            hand.add(game.getCardSource().next());
            return 2.0; // caller multiplies by BET
        }
        while (true) {
            if (isBust(BasicStrategyAdvisor.computeCount(hand))) break;
            Action a = BasicStrategyAdvisor.advise(hand, dealerUpcard, false);
            if (a == Action.STAND) break;
            hand.add(game.getCardSource().next());
        }
        return 1.0; // caller multiplies by BET
    }

    // Overload that returns actual wager amount
    private double playBasicHandWager(List<Card> hand, int dealerUpcard, Game game, double BET) {
        return playBasicHand(hand, dealerUpcard, game) * BET;
    }

    private double[] settleTwo(List<Card> hand1, double w1,
                                List<Card> hand2, double w2,
                                List<Card> dealerCards, boolean dealerBust,
                                Game game) {
        return new double[]{
            settleOne(hand1, w1, dealerCards, dealerBust, game),
            settleOne(hand2, w2, dealerCards, dealerBust, game)
        };
    }

    private double settleOne(List<Card> hand, double wagered,
                              List<Card> dealerCards, boolean dealerBust, Game game) {
        if (isBust(BasicStrategyAdvisor.computeCount(hand))) return 0;
        if (dealerBust) return wagered * DOUBLE_MULTI;
        int cmp = game.compareHands(dealerCards, hand);
        if (cmp > 0)       return wagered * DOUBLE_MULTI;
        else if (cmp == 0) return wagered * PUSH_MULTI;
        else               return 0;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean mirrorShouldHit(Count count) {
        int hard = count.getLeft(), soft = count.getRight();
        if (hard > BJ_CNT) return false;
        if (soft >= DEALER_THRESHOLD_17 && soft <= BJ_CNT) return false;
        if (hard >= DEALER_THRESHOLD_17) return false;
        return true;
    }

    private boolean isBust(Count count) {
        return count.getLeft() > BJ_CNT;
    }
}
