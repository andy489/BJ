package com.casino.blackjack.service.simulation;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Count;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.rng.RngCardSource;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;

/**
 * Headless blackjack simulator — no DB, no wallet, pure in-memory.
 *
 * Player strategy (mirrors dealer rule):
 *   - Hit on hard total ≤ 16
 *   - Hit on soft total ≤ 16 (soft 16 = Ace + 5 counting Ace as 11 = 16, but left=6)
 *   - Stand on hard 17+
 *   - No splits, no double-down, no insurance, no surrender
 *
 * Payouts mirror game rules:
 *   - Player BJ vs no dealer BJ → 2.5× bet returned (net +1.5)
 *   - Win → 2× bet returned (net +1)
 *   - Push → 1× bet returned (net 0)
 *   - Loss → 0 returned (net -1)
 */
@Service
public class SimulationService {

    private static final String STRATEGY_NAME =
            "Dealer mirror — hit ≤ soft/hard 16, stand ≥ hard 17; no splits/DD/insurance/surrender";

    public SimulationResult simulate(long n) {
        final double BET = 100.0;

        double totalWagered  = 0;
        double totalReturned = 0;
        long wins = 0, losses = 0, pushes = 0, blackjacks = 0;

        for (long i = 0; i < n; i++) {
            Game game = new Game();
            game.setCardSource(new RngCardSource());
            game.deal();

            List<Card> playerCards = game.getPlayerCards();
            List<Card> dealerCards = game.getDealerCards();

            // Hide dealer's second card exactly as the real game does
            Card dealerHidden = dealerCards.remove(1);

            totalWagered += BET;

            boolean playerBJ = game.checkBJCards(playerCards);

            // Player plays first (dealer-mirror strategy)
            if (!playerBJ) {
                while (shouldHit(game.getCount(playerCards))) {
                    playerCards.add(game.getCardSource().next());
                }
            }

            boolean playerBust = isBust(game.getCount(playerCards));

            // Dealer reveals hidden card and plays to 17+
            dealerCards.add(dealerHidden);
            game.dealerPlayUntilSoft17Public();

            boolean dealerBJ   = game.checkBJCards(dealerCards);
            boolean dealerBust = isBust(game.getCount(dealerCards));

            // Evaluate result
            double returned;
            if (playerBust) {
                returned = 0;
                losses++;
            } else if (playerBJ && !dealerBJ) {
                returned = BET * BJ_MULTI;  // 2.5×
                blackjacks++;
                wins++;
            } else if (playerBJ && dealerBJ) {
                returned = BET * PUSH_MULTI;  // push
                pushes++;
            } else if (dealerBust) {
                returned = BET * DOUBLE_MULTI;  // 2×
                wins++;
            } else {
                int compare = game.compareHands(dealerCards, playerCards);
                if (compare < 0) {          // player wins
                    returned = BET * DOUBLE_MULTI;
                    wins++;
                } else if (compare == 0) {  // push
                    returned = BET * PUSH_MULTI;
                    pushes++;
                } else {                    // dealer wins
                    returned = 0;
                    losses++;
                }
            }

            totalReturned += returned;
        }

        return SimulationResult.of(n, totalWagered, totalReturned,
                wins, losses, pushes, blackjacks, STRATEGY_NAME);
    }

    /** Dealer-mirror hit rule: hit on hard ≤ 16, or soft ≤ 16. */
    private boolean shouldHit(Count count) {
        int hard = count.getLeft();
        int soft = count.getRight();
        if (hard > BJ_CNT) return false;   // already bust — stop
        // Stand on hard 17+ (when soft == hard or soft > 21)
        if (soft >= DEALER_THRESHOLD_17 && soft <= BJ_CNT) return false;
        // Hard total ≥ 17 and no usable ace
        if (hard >= DEALER_THRESHOLD_17) return false;
        return true;
    }

    private boolean isBust(Count count) {
        return count.getLeft() > BJ_CNT;
    }
}
