package com.casino.blackjack.service.gamelogic.rng.scenario;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.rng.CardSource;
import com.casino.blackjack.service.gamelogic.rng.FixedCardSource;
import com.casino.blackjack.service.gamelogic.rng.RngCardSource;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;

/**
 * Card deal order for every FixedCardSource scenario: [0] dealer visible card [1] dealer hidden
 * card [2] player card 0 [3] player card 1 [4+] subsequent draws in order (player hit, then dealer
 * hit, alternating)
 * <p>
 * FixedCardSource repeats the sequence once exhausted, so every scenario works across multiple
 * consecutive hands.
 * <p>
 * To add a new scenario: add an enum constant and implement source(). Switch the active scenario in
 * CardSourceConfig.
 */
public enum DeckScenario {

  RANDOM("Normal random play") {
    @Override
    public CardSource source() {
      return new RngCardSource();
    }
  },

  // ── Blackjack / Insurance scenarios ───────────────────────────────────────

  // Player BJ (Ace+King) vs dealer Ace + hidden King → even-money offered
  PLAYER_BJ_DEALER_HIDDEN_BJ("Player BJ vs dealer hidden BJ — even-money offer") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, KING_RANK), // dealer visible
          Card.of(HEARTS_SUIT, ACE_RANK),  // dealer hidden → BJ
          Card.of(CLUBS_SUIT, ACE_RANK),   // player card 0
          Card.of(CLUBS_SUIT, QUEEN_RANK)    // player card 1 → BJ
      );
    }
  },

  // Player BJ vs dealer Ace + hidden 9 → insurance offered, player wins with BJ payout
  PLAYER_BJ_DEALER_ACE_NO_BJ("Player BJ vs dealer Ace (no hidden BJ) — insurance offer") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, ACE_RANK),   // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),  // dealer hidden → 20
          Card.of(SPADES_SUIT, ACE_RANK),   // player card 0
          Card.of(DIAMONDS_SUIT, KING_RANK)   // player card 1 → BJ
      );
    }
  },

  // ── Double-down scenarios ──────────────────────────────────────────────────

  // Player 5+6 (11) vs dealer 6 — textbook double-down situation, dealer busts
  PLAYER_11_DEALER_6_BUST("Player 11 vs dealer 6 — dealer busts on hit") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, SIX_RANK),   // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),  // dealer hidden → 15
          Card.of(SPADES_SUIT, FIVE_RANK),  // player card 0
          Card.of(DIAMONDS_SUIT, SIX_RANK),   // player card 1 → 11
          Card.of(CLUBS_SUIT, TEN_RANK),   // player double/hit → 21
          Card.of(HEARTS_SUIT, EIGHT_RANK)  // dealer hit → 23 bust
      );
    }
  },

  // Player 4+2 (6) vs dealer 10 — double NOT recommended by basic strategy → confirm prompt
  PLAYER_6_DEALER_10_DD_NOT_BASIC("Player 6 vs dealer 10 — double-down not basic strategy") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, TEN_RANK),    // dealer visible
          Card.of(HEARTS_SUIT, SEVEN_RANK), // dealer hidden → 17
          Card.of(SPADES_SUIT,  FOUR_RANK),  // player card 0
          Card.of(DIAMONDS_SUIT, TWO_RANK), // player card 1 → 6
          Card.of(CLUBS_SUIT, TEN_RANK)     // double card → 21
      );
    }
  },

  // ── Hit / Bust scenarios ───────────────────────────────────────────────────

  // Player 6+6 (12) vs dealer Queen — hits: +3 → 15, +6 → 21; dealer stands on 20
  PLAYER_12_DEALER_QUEEN_HIT_TO_21("Player 12 vs dealer Queen — hits to 21") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, QUEEN_RANK), // dealer visible
          Card.of(HEARTS_SUIT, TEN_RANK),   // dealer hidden → 20
          Card.of(SPADES_SUIT, SIX_RANK),   // player card 0
          Card.of(DIAMONDS_SUIT, SIX_RANK),   // player card 1 → 12
          Card.of(CLUBS_SUIT, THREE_RANK),  // hit 1 → 15
          Card.of(HEARTS_SUIT, SIX_RANK)    // hit 2 → 21
      );
    }
  },

  // Player 10+7 (17) vs dealer 6 — hard 17, triggers confirm prompt on hit
  PLAYER_HARD_17_DEALER_6("Player hard 17 vs dealer 6 — hit-on-hard-17 confirm") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, SIX_RANK),   // dealer visible
          Card.of(HEARTS_SUIT, FIVE_RANK),  // dealer hidden → 11
          Card.of(SPADES_SUIT, TEN_RANK),   // player card 0
          Card.of(DIAMONDS_SUIT, SEVEN_RANK), // player card 1 → 17
          Card.of(CLUBS_SUIT, THREE_RANK),  // hit → 20
          Card.of(HEARTS_SUIT, TEN_RANK)    // dealer hit → 21
      );
    }
  },

  // ── Soft hand scenarios ────────────────────────────────────────────────────

  // Player Ace+6 (soft 17) vs dealer 5 — soft hand, hit safely to 21
  PLAYER_SOFT_17_DEALER_5("Player soft 17 (Ace+6) vs dealer 5 — soft hand hit") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, FIVE_RANK),  // dealer visible
          Card.of(HEARTS_SUIT, FOUR_RANK),  // dealer hidden → 9, must hit → +10 = 19
          Card.of(SPADES_SUIT, ACE_RANK),   // player card 0
          Card.of(DIAMONDS_SUIT, SIX_RANK),   // player card 1 → soft 17
          Card.of(CLUBS_SUIT, FOUR_RANK),  // player hit → soft 21
          Card.of(HEARTS_SUIT, TEN_RANK)    // dealer hit → 19
      );
    }
  },

  // ── Surrender scenario ─────────────────────────────────────────────────────

  // Player 10+6 (16) vs dealer Ace — classic surrender situation, dealer has 20
  PLAYER_16_DEALER_ACE_SURRENDER("Player 16 vs dealer Ace — surrender scenario") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, ACE_RANK),   // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),  // dealer hidden → 20
          Card.of(SPADES_SUIT, TEN_RANK),   // player card 0
          Card.of(DIAMONDS_SUIT, SIX_RANK)    // player card 1 → 16
      );
    }
  },

  // ── Split scenario ─────────────────────────────────────────────────────────

  // Player 8+8 vs dealer 6 — four consecutive re-splits (hits the maxSplits=4 cap on the 5th attempt).
  // After each split the active hand draws an 8, making it a re-splittable pair again.
  // On the 4th split hand0 still holds 8+8, but splitCount==4 so the split choice must not appear.
  // All five hands stand on safe totals; dealer busts with a draw.
  PLAYER_PAIR_8_DEALER_6("Player 8+8 vs dealer 6 — four re-splits, 5th blocked by maxSplits cap") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, SIX_RANK),        // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),       // dealer hidden → 15
          Card.of(SPADES_SUIT, EIGHT_RANK),      // player card 0
          Card.of(DIAMONDS_SUIT, EIGHT_RANK),    // player card 1 → pair 8s (splitCount=0)
          // ── split 1 (hand0 active, splitCount→1) ──
          Card.of(CLUBS_SUIT, EIGHT_RANK),       // hand0 draws → 8+8, still a pair
          Card.of(HEARTS_SUIT, ACE_RANK),        // hand1 (new) draws → 8+11=19 (soft)
          // ── split 2 (hand0 active, splitCount→2) ──
          Card.of(SPADES_SUIT, EIGHT_RANK),      // hand0 draws → 8+8, still a pair
          Card.of(DIAMONDS_SUIT, TEN_RANK),      // hand2 (new) draws → 8+10=18
          // ── split 3 (hand0 active, splitCount→3) ──
          Card.of(CLUBS_SUIT, EIGHT_RANK),       // hand0 draws → 8+8, still a pair
          Card.of(SPADES_SUIT, TEN_RANK),        // hand3 (new) draws → 8+10=18
          // ── split 4 (hand0 active, splitCount→4 = maxSplits) ──
          Card.of(DIAMONDS_SUIT, EIGHT_RANK),    // hand0 draws → 8+8, pair but split now blocked
          Card.of(CLUBS_SUIT, TEN_RANK),         // hand4 (new) draws → 8+10=18
          // ── play out hands 0-4 (all stand, no hits needed) ──
          // hand0 stands on 16 (no further action taken in test)
          // ── dealer draws to bust ──
          Card.of(HEARTS_SUIT, EIGHT_RANK)       // dealer draws → 15+8=23, bust
      );
    }
  },

  // Player Ace+Ace vs dealer 6 — split aces: each hand gets exactly ONE card, no BJ payout
  PLAYER_PAIR_ACE_DEALER_6("Player A+A vs dealer 6 — split aces (one card each, no BJ)") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, SIX_RANK),       // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),     // dealer hidden → 15
          Card.of(SPADES_SUIT, ACE_RANK),      // player card 0
          Card.of(DIAMONDS_SUIT, ACE_RANK),    // player card 1 → pair of aces
          Card.of(CLUBS_SUIT, KING_RANK),      // hand 0 (main) one card → A+K = 21 (not BJ)
          Card.of(HEARTS_SUIT, NINE_RANK),     // hand 1 one card → A+9 = 20
          Card.of(HEARTS_SUIT, EIGHT_RANK)     // dealer hit → 23 bust
      );
    }
  },

  // Player 8+8 vs dealer 6 — first split deals another 8 to hand 0 → re-split to three hands.
  // Draw order after the initial 4 cards follows initSplit: on each split the ACTIVE hand
  // draws first, then the newly created hand draws.
  PLAYER_PAIR_8_RESPLIT_DEALER_6("Player 8+8 vs dealer 6 — re-split to three hands") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, SIX_RANK),       // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),     // dealer hidden → 15
          Card.of(SPADES_SUIT, EIGHT_RANK),    // player card 0
          Card.of(DIAMONDS_SUIT, EIGHT_RANK),  // player card 1 → pair 8s
          // ── first split (hand0 active) ──
          Card.of(HEARTS_SUIT, EIGHT_RANK),    // hand 0 draws → 8+8 = pair again (re-splittable)
          Card.of(CLUBS_SUIT, TEN_RANK),       // hand 1 (new) draws → 8+10 = 18
          // ── re-split of hand0 (hand0 still active) ──
          Card.of(SPADES_SUIT, THREE_RANK),    // hand 0 draws → 8+3 = 11
          Card.of(DIAMONDS_SUIT, TEN_RANK),    // hand 2 (new, inserted at idx1) draws → 8+10 = 18
          // ── player hits per hand, in play order (0, 1, 2) ──
          Card.of(CLUBS_SUIT, KING_RANK),      // hand 0 hit → 11+10 = 21, stand
          Card.of(HEARTS_SUIT, TWO_RANK),      // hand 1 hit → 18+2 = 20, stand
          Card.of(HEARTS_SUIT, EIGHT_RANK)     // dealer hit → 15+8 = 23 bust
      );
    }
  },

  // Player 5+5 vs dealer Queen — split: hand0 draws K→15, hand1 draws 7→12 then hits→22 bust
  PLAYER_PAIR_5_DEALER_QUEEN("Player 5+5 vs dealer Queen — split, hit on hand0") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, QUEEN_RANK),     // dealer visible
          Card.of(HEARTS_SUIT, TEN_RANK),      // dealer hidden → 20
          Card.of(SPADES_SUIT, FIVE_RANK),     // player card 0
          Card.of(DIAMONDS_SUIT, FIVE_RANK),   // player card 1 → pair 5s
          Card.of(CLUBS_SUIT, KING_RANK),      // hand1 (active first) draws → 5+K = 15
          Card.of(HEARTS_SUIT, SEVEN_RANK),    // hand0 draws → 5+7 = 12
          Card.of(SPADES_SUIT, THREE_RANK),    // hand0 hit → 12+3 = 15
          Card.of(DIAMONDS_SUIT, TEN_RANK)     // hand0 hit → 15+10 = 25 bust
      );
    }
  },

  // Player 9♠+9♣ vs dealer 6 — three splits, hand3 busts, hand1 hits to 18 with an Ace, dealer busts.
  //
  // Split tree:
  //   deal:    hand0=9♠, hand1=9♣
  //   split1:  hand0 draws 9♦ → 9+9=18 (re-split); hand1 draws 2♥ → 9+2=11
  //   split2 (hand0 active): hand0 draws Q♣ → 9+Q=19 (stand); hand2 draws 9♥ → 9+9=18 (re-split)
  //   split3 (hand2 active): hand2 draws J♠ → 9+J=19 (stand); hand3 draws 3♦ → 9+3=12 (must hit)
  //
  // Play order (left to right):
  //   hand0 stand 19
  //   hand2 stand 19
  //   hand3 hits K → 12+10=22 BUST
  //   hand1 hits A♦ → soft 12 (counted as 12), hits 6♣ → 18 → stand  (4 cards: 9,2,A,6)
  // Dealer: 6+9=15 → draws 8 → 23 bust → hand0/hand2/hand1 win, hand3 loses
  SPLIT_RESPLIT_HIT_BUST_DOUBLE("9+9 vs 6 — three splits, hand3 busts, hand1 hits Ace to 18, dealer busts") {
    @Override
    public CardSource source() {
      return new FixedCardSource(
          Card.of(CLUBS_SUIT, SIX_RANK),          // dealer visible
          Card.of(HEARTS_SUIT, NINE_RANK),         // dealer hidden → 15
          Card.of(SPADES_SUIT, NINE_RANK),         // player card 0
          Card.of(CLUBS_SUIT, NINE_RANK),          // player card 1 → pair 9s
          // ── split 1: hand0 (left) draws [4], hand1 (right) draws [5] ──
          Card.of(DIAMONDS_SUIT, NINE_RANK),       // hand0: 9+9 = 18 → re-split offered
          Card.of(HEARTS_SUIT, TWO_RANK),          // hand1: 9+2 = 11
          // ── split 2 (hand0 active): hand0 draws [6], hand2 (new right) draws [7] ──
          Card.of(CLUBS_SUIT, QUEEN_RANK),         // hand0: 9+Q = 19 → stand
          Card.of(HEARTS_SUIT, NINE_RANK),         // hand2: 9+9 = 18 → re-split offered
          // ── split 3 (hand2 active): hand2 draws [8], hand3 (new right) draws [9] ──
          Card.of(SPADES_SUIT, JAKE_RANK),         // hand2: 9+J = 19 → stand
          Card.of(HEARTS_SUIT, KING_RANK),          // hand3 hit → 12+10 = 22 bust
          // ── play hands left to right ──
          // hand0 stands on 19 (no card needed)
          // hand2 stands on 19 (no card needed)
          // hand3 hits → bust ──
          // hand1 hits twice to 18 (4 cards: 9,2,A,6) ──
          Card.of(DIAMONDS_SUIT, THREE_RANK),      // hand3: 9+3 = 12 → must hit
          Card.of(DIAMONDS_SUIT, ACE_RANK),        // hand1 hit → 9+2+A = soft 12 (counted as 12)
          Card.of(CLUBS_SUIT, TEN_RANK),           // hand1 hit → 12+6 = 18 → stand
          // ── dealer draws ──
          Card.of(SPADES_SUIT, EIGHT_RANK)         // dealer → 15+8 = 23 bust
      );
    }
  };

  final String description;

  DeckScenario(String description) {
    this.description = description;
  }

  public abstract CardSource source();
}
