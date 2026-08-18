package com.casino.blackjack.service.gamelogic.rng.scenario;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.rng.CardSource;
import com.casino.blackjack.service.gamelogic.rng.FixedCardSource;
import com.casino.blackjack.service.gamelogic.rng.RngCardSource;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;

/**
 * Card deal order for every FixedCardSource scenario:
 *   [0] dealer visible card
 *   [1] dealer hidden card
 *   [2] player card 0
 *   [3] player card 1
 *   [4+] subsequent draws in order (player hit, then dealer hit, alternating)
 *
 * FixedCardSource repeats the sequence once exhausted, so every scenario
 * works across multiple consecutive hands.
 *
 * To add a new scenario: add an enum constant and implement source().
 * Switch the active scenario in CardSourceConfig.
 */
public enum DeckScenario {

    RANDOM("Normal random play") {
        @Override public CardSource source() { return new RngCardSource(); }
    },

    // ── Blackjack / Insurance scenarios ───────────────────────────────────────

    // Player BJ (Ace+King) vs dealer Ace + hidden King → even-money offered
    PLAYER_BJ_DEALER_HIDDEN_BJ("Player BJ vs dealer hidden BJ — even-money offer") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    KING_RANK), // dealer visible
                    Card.of(HEARTS_SUIT,   ACE_RANK),  // dealer hidden → BJ
                    Card.of(CLUBS_SUIT,   ACE_RANK),   // player card 0
                    Card.of(CLUBS_SUIT, QUEEN_RANK)    // player card 1 → BJ
            );
        }
    },

    // Player BJ vs dealer Ace + hidden 9 → insurance offered, player wins with BJ payout
    PLAYER_BJ_DEALER_ACE_NO_BJ("Player BJ vs dealer Ace (no hidden BJ) — insurance offer") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    ACE_RANK),   // dealer visible
                    Card.of(HEARTS_SUIT,   NINE_RANK),  // dealer hidden → 20
                    Card.of(SPADES_SUIT,   ACE_RANK),   // player card 0
                    Card.of(DIAMONDS_SUIT, KING_RANK)   // player card 1 → BJ
            );
        }
    },

    // ── Double-down scenarios ──────────────────────────────────────────────────

    // Player 5+6 (11) vs dealer 6 — textbook double-down situation, dealer busts
    PLAYER_11_DEALER_6_BUST("Player 11 vs dealer 6 — dealer busts on hit") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    SIX_RANK),   // dealer visible
                    Card.of(HEARTS_SUIT,   NINE_RANK),  // dealer hidden → 15
                    Card.of(SPADES_SUIT,   FIVE_RANK),  // player card 0
                    Card.of(DIAMONDS_SUIT, SIX_RANK),   // player card 1 → 11
                    Card.of(CLUBS_SUIT,    TEN_RANK),   // player double/hit → 21
                    Card.of(HEARTS_SUIT,   EIGHT_RANK)  // dealer hit → 23 bust
            );
        }
    },

    // Player 9+2 (11) vs dealer 10 — double NOT recommended by basic strategy → confirm prompt
    PLAYER_11_DEALER_10_DD_NOT_BASIC("Player 11 vs dealer 10 — double-down not basic strategy") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    TEN_RANK),   // dealer visible
                    Card.of(HEARTS_SUIT,   SEVEN_RANK), // dealer hidden → 17
                    Card.of(SPADES_SUIT,   NINE_RANK),  // player card 0
                    Card.of(DIAMONDS_SUIT, TWO_RANK),   // player card 1 → 11
                    Card.of(CLUBS_SUIT,    TEN_RANK)    // double card → 21
            );
        }
    },

    // ── Hit / Bust scenarios ───────────────────────────────────────────────────

    // Player 6+6 (12) vs dealer Queen — hits: +3 → 15, +6 → 21; dealer stands on 20
    PLAYER_12_DEALER_QUEEN_HIT_TO_21("Player 12 vs dealer Queen — hits to 21") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    QUEEN_RANK), // dealer visible
                    Card.of(HEARTS_SUIT,   TEN_RANK),   // dealer hidden → 20
                    Card.of(SPADES_SUIT,   SIX_RANK),   // player card 0
                    Card.of(DIAMONDS_SUIT, SIX_RANK),   // player card 1 → 12
                    Card.of(CLUBS_SUIT,    THREE_RANK),  // hit 1 → 15
                    Card.of(HEARTS_SUIT,   SIX_RANK)    // hit 2 → 21
            );
        }
    },

    // Player 10+7 (17) vs dealer 6 — hard 17, triggers confirm prompt on hit
    PLAYER_HARD_17_DEALER_6("Player hard 17 vs dealer 6 — hit-on-hard-17 confirm") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    SIX_RANK),   // dealer visible
                    Card.of(HEARTS_SUIT,   FIVE_RANK),  // dealer hidden → 11
                    Card.of(SPADES_SUIT,   TEN_RANK),   // player card 0
                    Card.of(DIAMONDS_SUIT, SEVEN_RANK), // player card 1 → 17
                    Card.of(CLUBS_SUIT,    THREE_RANK),  // hit → 20
                    Card.of(HEARTS_SUIT,   TEN_RANK)    // dealer hit → 21
            );
        }
    },

    // ── Soft hand scenarios ────────────────────────────────────────────────────

    // Player Ace+6 (soft 17) vs dealer 5 — soft hand, hit safely to 21
    PLAYER_SOFT_17_DEALER_5("Player soft 17 (Ace+6) vs dealer 5 — soft hand hit") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    FIVE_RANK),  // dealer visible
                    Card.of(HEARTS_SUIT,   FOUR_RANK),  // dealer hidden → 9, must hit → +10 = 19
                    Card.of(SPADES_SUIT,   ACE_RANK),   // player card 0
                    Card.of(DIAMONDS_SUIT, SIX_RANK),   // player card 1 → soft 17
                    Card.of(CLUBS_SUIT,    FOUR_RANK),  // player hit → soft 21
                    Card.of(HEARTS_SUIT,   TEN_RANK)    // dealer hit → 19
            );
        }
    },

    // ── Surrender scenario ─────────────────────────────────────────────────────

    // Player 10+6 (16) vs dealer Ace — classic surrender situation, dealer has 20
    PLAYER_16_DEALER_ACE_SURRENDER("Player 16 vs dealer Ace — surrender scenario") {
        @Override public CardSource source() {
            return new FixedCardSource(
                    Card.of(CLUBS_SUIT,    ACE_RANK),   // dealer visible
                    Card.of(HEARTS_SUIT,   NINE_RANK),  // dealer hidden → 20
                    Card.of(SPADES_SUIT,   TEN_RANK),   // player card 0
                    Card.of(DIAMONDS_SUIT, SIX_RANK)    // player card 1 → 16
            );
        }
    };

    final String description;

    DeckScenario(String description) { this.description = description; }

    public abstract CardSource source();
}
