package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.config.GameProperties;
import com.casino.blackjack.config.PaytableProperties;
import com.casino.blackjack.service.BasicStrategy;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.processor.DoubleDownConfirmProcessor;
import com.casino.blackjack.service.gamelogic.processor.GameContext;
import com.casino.blackjack.service.gamelogic.processor.GameStateProcessorChain;
import com.casino.blackjack.service.gamelogic.rng.FixedCardSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — no Spring context, no database, no RNG.
 *
 * Deal order for FixedCardSource:
 *   position 0 → dealer visible card
 *   position 1 → dealer hidden card (moved to dealerSecondCard after adjustDealerCardsAfterDeal)
 *   position 2 → player card 0
 *   position 3 → player card 1
 *   position 4+ → subsequent hits in request order (player hit, dealer hit, etc.)
 *
 * Processors that only mutate the Game DTO are testable with null wallet/repo fields.
 * Processors that require wallet or repo (DoubleDownBetProcessor, DoubleDownYesWalletProcessor)
 * are isolated from tests by setting up Game state directly — bypassing repo calls while
 * still exercising every card-dealing and result-evaluation code path.
 */
class GameScenarioTest {

    private static final GameStateProcessorChain CHAIN;

    static {
        GameProperties props = new GameProperties();
        props.setMaxSplits(4);
        CHAIN = new GameStateProcessorChain(props);
    }

    private static final DoubleDownConfirmProcessor DD_CONFIRM = new DoubleDownConfirmProcessor();
    private static final BasicStrategy BASIC_STRATEGY = new BasicStrategy();
    private static final PaytableProperties PT = new PaytableProperties();

    /** Minimal context for pure-logic tests — all wallet/repo fields are null (unused by pure processors). */
    private static GameContext ctx(Game game) {
        return new GameContext(game, null, null, null, null, null, null, null, null, null, 4, 3000, PT);
    }

    /** Convenience: build a freshly-dealt game with fixed cards and run the processor chain. */
    private Game deal(Card... cards) {
        Game game = new Game();
        game.setCardSource(new FixedCardSource(cards));
        game.setDealt(true);
        game.deal();
        game.makeChoice(CHOICE_DEAL);
        game.adjustDealerCardsAfterDeal();
        CHAIN.process(ctx(game));
        return game;
    }

    /**
     * Simulates what DoubleDownBetProcessor does when BasicStrategy returns false:
     * records CHOICE_DOUBLE_NOT_BASIC_STRATEGY and sets available choices to YES/NO.
     * No wallet or repo calls happen in that path — we replicate the state directly.
     */
    private static void applyNotBasicStrategyPrompt(Game game) {
        game.makeChoice(CHOICE_DOUBLE_NOT_BASIC_STRATEGY)
                .setAvailableChoices(List.of(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO))
                .setDoubleDown(false);
    }

    /**
     * Simulates what DoubleDownYesWalletProcessor does (wallet doubling) without touching a repo.
     * Sets doubleDown=true so DoubleDownConfirmProcessor's YES branch can proceed.
     */
    private static void applyWalletDoubleState(Game game) {
        game.setDoubleDown(true);
    }

    // -------------------------------------------------------------------------
    // Scenario: PLAYER_BJ_DEALER_HIDDEN_BJ
    //   Dealer visible: Ace | Dealer hidden: King → dealer BJ
    //   Player: Ace + King → player BJ
    //   Sub-scenario A: even-money offer is presented after deal
    //   Sub-scenario B: player declines even money → dealer reveals BJ → push (bet returned)
    //   Sub-scenario C: player accepts even money → guaranteed 2:1 payout
    // -------------------------------------------------------------------------
    @Test
    void playerBJ_dealerHiddenBJ_evenMoneyOffered() {
        Game game = deal(
                Card.of(CLUBS_SUIT, ACE_RANK),    // dealer visible
                Card.of(HEARTS_SUIT, KING_RANK),  // dealer hidden → BJ
                Card.of(SPADES_SUIT, ACE_RANK),   // player card 0
                Card.of(DIAMONDS_SUIT, KING_RANK) // player card 1 → BJ
        );

        assertThat(game.getAvailableChoices())
                .containsExactly(CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO);
    }

    @Test
    void playerBJ_dealerHiddenBJ_declineEvenMoney_push() {
        Game game = deal(
                Card.of(CLUBS_SUIT, ACE_RANK),
                Card.of(HEARTS_SUIT, KING_RANK),
                Card.of(SPADES_SUIT, ACE_RANK),
                Card.of(DIAMONDS_SUIT, KING_RANK)
        );

        game.makeChoice(CHOICE_EVEN_MONEY_NO);
        CHAIN.process(ctx(game));

        // Both player and dealer have BJ → push: bet returned (1×)
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(PUSH_MULTI);
        assertThat(game.getAvailableChoices())
                .contains(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void playerBJ_dealerHiddenBJ_acceptEvenMoney_paysDouble() {
        Game game = deal(
                Card.of(CLUBS_SUIT, ACE_RANK),
                Card.of(HEARTS_SUIT, KING_RANK),
                Card.of(SPADES_SUIT, ACE_RANK),
                Card.of(DIAMONDS_SUIT, KING_RANK)
        );

        game.makeChoice(CHOICE_EVEN_MONEY_YES);
        CHAIN.process(ctx(game));

        // Even money accepted → 2:1 payout guaranteed regardless of dealer outcome
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getAvailableChoices())
                .contains(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    // -------------------------------------------------------------------------
    // Scenario: PLAYER_BJ_DEALER_ACE_NO_BJ
    //   Dealer visible: Ace | Dealer hidden: 9 → dealer total 20 (no BJ)
    //   Player: Ace + King → player BJ
    //   Sub-scenario A: even-money offer is presented after deal (not finalized yet)
    //   Sub-scenario B: player declines even money → dealer no BJ → player BJ pays 3:2
    //   Sub-scenario C: player accepts even money → 2:1 regardless of dealer outcome
    //   Sub-scenario D: player BJ vs dealer upcard that cannot make BJ → auto-win, no prompt
    // -------------------------------------------------------------------------
    @Test
    void playerBJ_dealerAceNoHiddenBJ_evenMoneyOffered_notFinalizedYet() {
        Game game = deal(
                Card.of(CLUBS_SUIT, ACE_RANK),    // dealer visible
                Card.of(HEARTS_SUIT, NINE_RANK),  // dealer hidden → 20 (no BJ)
                Card.of(SPADES_SUIT, ACE_RANK),   // player card 0
                Card.of(DIAMONDS_SUIT, KING_RANK) // player card 1 → BJ
        );

        // Even-money offer pending — hand must NOT be finalized yet
        assertThat(game.getAvailableChoices())
                .containsExactly(CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO);
        assertThat(game.getFinalized()).isFalse();
    }

    @Test
    void playerBJ_dealerAceNoHiddenBJ_declineEvenMoney_playerBJWins() {
        Game game = deal(
                Card.of(CLUBS_SUIT, ACE_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, ACE_RANK),
                Card.of(DIAMONDS_SUIT, KING_RANK)
        );

        game.makeChoice(CHOICE_EVEN_MONEY_NO);
        CHAIN.process(ctx(game));

        // Dealer has no BJ → player BJ beats dealer → 3:2 payout (2.5×)
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(PT.bjMulti());
        assertThat(game.getAvailableChoices())
                .contains(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void playerBJ_dealerAceNoHiddenBJ_acceptEvenMoney_paysDouble() {
        Game game = deal(
                Card.of(CLUBS_SUIT, ACE_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, ACE_RANK),
                Card.of(DIAMONDS_SUIT, KING_RANK)
        );

        game.makeChoice(CHOICE_EVEN_MONEY_YES);
        CHAIN.process(ctx(game));

        // Even money accepted → 2:1 payout regardless of dealer outcome
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getAvailableChoices())
                .contains(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void playerBJ_dealerNonAceUpcard_evenMoneyNotOffered_autoWinBJPayout() {
        // Dealer visible: 6 (cannot make BJ) → no even-money prompt, player BJ auto-wins at 3:2
        // After deal the processor sees dealerCards still has 2 cards so dealerCannotMakeBJ()
        // fires via the even-money branch — dealer upcard 6 means no BJ concern, but the
        // processor chain routes to PlayerBlackjackAfterDealProcessor which offers even-money
        // only when dealer COULD make BJ (Ace upcard). With 6 upcard, dealerFirstCardCannotMakeBJ
        // is evaluated against dealerCards.size() at chain time (2 cards) so always false.
        // The actual production auto-win path: even-money still presented for Ace upcard only.
        // For non-Ace dealer upcard with player BJ: no even-money, game finalizes immediately.
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),    // dealer visible — non-Ace, cannot make BJ
                Card.of(HEARTS_SUIT, NINE_RANK),  // dealer hidden → 15, hits → bust
                Card.of(SPADES_SUIT, ACE_RANK),   // player card 0
                Card.of(DIAMONDS_SUIT, KING_RANK), // player card 1 → BJ
                Card.of(CLUBS_SUIT, SEVEN_RANK)   // dealer hit → 22 (bust, not needed here)
        );

        // Dealer upcard 6 cannot make BJ → player BJ auto-wins, game finalized at deal time
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(PT.bjMulti());
        // No even-money choice — settled immediately
        assertThat(game.getAvailableChoices())
                .doesNotContain(CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO);
    }

    // -------------------------------------------------------------------------
    // Scenario 2: player has 11 (5+6), dealer shows 6 with unknown hidden card
    //   → player should be offered Double Down among other choices
    //   → player hits and reaches 21 → player wins
    // -------------------------------------------------------------------------
    @Test
    void player11_dealerShows6_playerHitsTo21_playerWins() {
        // dealer visible: 6, dealer hidden: 9 (total 15 → must hit → gets e.g. 8 → bust)
        // player: 5 + 6 = 11
        // next card for player hit: 10 → player reaches 21
        // dealer plays out: 6 + 9 = 15 (below 17, must hit) → next dealer card: 8 → 23 (bust)
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),    // dealer visible
                Card.of(HEARTS_SUIT, NINE_RANK),   // dealer hidden
                Card.of(SPADES_SUIT, FIVE_RANK),   // player card 0
                Card.of(DIAMONDS_SUIT, SIX_RANK),  // player card 1  → player: 11
                Card.of(CLUBS_SUIT, TEN_RANK),     // player hit → 21
                Card.of(HEARTS_SUIT, EIGHT_RANK)   // dealer hit → 6+9+8 = 23 (bust)
        );

        // Normal deal: no BJ, dealer upcard is 6 (not Ace) → Surrender / Stand / Hit / Double
        assertThat(game.getAvailableChoices())
                .contains(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN, CHOICE_SURRENDER);

        // Player hits
        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));

        // Player reached 21 → dealer plays out, hand finalized, player wins
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getAvailableChoices())
                .containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void player11_dealerShows6_playerStands_dealerBusts_playerWins() {
        // dealer visible: 6, dealer hidden: 9 → 15, then hits 8 → bust
        // player: 5 + 6 = 11, stands immediately
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, FIVE_RANK),
                Card.of(DIAMONDS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, EIGHT_RANK)  // dealer hits → bust
        );

        assertThat(game.getAvailableChoices()).contains(CHOICE_STAND);

        game.makeChoice(CHOICE_STAND);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
    }

    // =========================================================================
    // Scenario: PLAYER_11_DEALER_6_BUST
    //   Cards: dealer visible=6, dealer hidden=9 (total 15, must hit)
    //          player=5+6=11 (textbook double-down hand)
    //          player double/hit draw=Ten → player 21
    //          dealer hit=8 → 6+9+8=23 bust
    // =========================================================================

    @Test
    void player11_dealer6_afterDeal_correctInitialState() {
        // Verifies: 4 cards drawn, dealer shows 6, hidden card stored, player has 5+6
        // availableChoices contains Surrender + Stand + Hit + Double (no pair, no Ace upcard)
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),    // dealer visible
                Card.of(HEARTS_SUIT, NINE_RANK),   // dealer hidden
                Card.of(SPADES_SUIT, FIVE_RANK),   // player 0
                Card.of(DIAMONDS_SUIT, SIX_RANK),  // player 1
                Card.of(CLUBS_SUIT, TEN_RANK),     // player double/hit
                Card.of(HEARTS_SUIT, EIGHT_RANK)   // dealer hit
        );

        // dealer visible only (hidden in dealerSecondCard)
        assertThat(game.getDealerCards()).hasSize(1);
        assertThat(game.getDealerCards().get(0).getRank()).isEqualTo(SIX_RANK);
        assertThat(game.getDealerSecondCard()).isNotNull();
        assertThat(game.getDealerSecondCard().getRank()).isEqualTo(NINE_RANK);

        // player has two cards summing to 11
        assertThat(game.getPlayerCards()).hasSize(2);
        assertThat(game.getPlayerCards().get(0).getRank()).isEqualTo(FIVE_RANK);
        assertThat(game.getPlayerCards().get(1).getRank()).isEqualTo(SIX_RANK);
        assertThat(game.playerHardScore()).isEqualTo(11);
        assertThat(game.playerScore()).isEqualTo("11");

        // not yet finalized
        assertThat(game.getFinalized()).isFalse();
        assertThat(game.getDoubleDown()).isFalse();
        assertThat(game.getHandMultiplier()).isEqualTo(0.0);

        // initial choices: Surrender offered (dealer is not Ace), no Split (no pair)
        assertThat(game.getAvailableChoices())
                .containsExactlyInAnyOrder(CHOICE_SURRENDER, CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN, CHOICE_AUTO_PLAY);
        assertThat(game.getAvailableChoices()).doesNotContain(CHOICE_SPLIT);

        // takenChoices recorded the initial DEAL
        assertThat(game.getTakenChoices()).containsExactly(CHOICE_DEAL);
    }

    @Test
    void player11_dealer6_double_basicStrategy_playerWins() {
        // BasicStrategy: player 11 vs dealer 6 → double recommended (true).
        // We exercise the card-dealing + result logic by driving DoubleDownConfirmProcessor YES
        // directly. This is equivalent to what DoubleDownBetProcessor does after the wallet deduction:
        // it sets finalized=true, doubleDown=true, hits the player, then evaluates the result.
        // Player: 5+6+Ten = 21, dealer: 6+9=15 must hit → +8 = 23 bust → player wins (DOUBLE_MULTI)
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, FIVE_RANK),
                Card.of(DIAMONDS_SUIT, SIX_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK),    // player double card → 21
                Card.of(HEARTS_SUIT, EIGHT_RANK)  // dealer hit → 23 bust
        );

        // Confirm basic strategy recommends doubling here
        assertThat(BASIC_STRATEGY.getDoubleDown(game)).isTrue();

        // Drive the confirm-YES path: DoubleDownConfirmProcessor handles CHOICE_DOUBLE_DOWN_YES
        // (wallet was already doubled by DoubleDownYesWalletProcessor upstream)
        game.makeChoice(CHOICE_DOUBLE_DOWN_YES);
        DD_CONFIRM.process(ctx(game));

        // Hand finalized immediately
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getDoubleDown()).isTrue();

        // Player received exactly one more card (3 total): 5+6+Ten=21
        assertThat(game.getPlayerCards()).hasSize(3);
        assertThat(game.getPlayerCards().get(2).getRank()).isEqualTo(TEN_RANK);
        assertThat(game.playerHardScore()).isEqualTo(21);

        // Dealer revealed hidden card and hit to bust (3 total: 6, 9, 8)
        assertThat(game.getDealerCards()).hasSize(3);
        assertThat(game.getDealerCards().get(0).getRank()).isEqualTo(SIX_RANK);
        assertThat(game.getDealerCards().get(1).getRank()).isEqualTo(NINE_RANK); // was dealerSecondCard
        assertThat(game.getDealerCards().get(2).getRank()).isEqualTo(EIGHT_RANK);
        assertThat(game.checkBust(game.getDealerCards())).isTrue();

        // Dealer busted → player wins at 2× (DOUBLE_MULTI)
        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void player11_dealer6_double_playerBusts_playerLoses() {
        // Player doubles on a hand where the next card causes bust.
        // Since 11+any_one_card cannot exceed 21 with a standard card (max rank counts as 10),
        // we test the bust-on-double path via a hand where the double card pushes past 21.
        // Player: Ten+Eight=18, double card=Five → 23 bust.
        // We drive DoubleDownConfirmProcessor YES directly to isolate the card/bust logic.
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),     // dealer visible
                Card.of(HEARTS_SUIT, NINE_RANK),    // dealer hidden (peeked on player bust)
                Card.of(SPADES_SUIT, TEN_RANK),     // player 0 → 10
                Card.of(DIAMONDS_SUIT, EIGHT_RANK), // player 1 → 18
                Card.of(CLUBS_SUIT, FIVE_RANK)      // player double → 23 bust
        );

        // Drive the confirm-YES path directly (wallet was already handled upstream)
        game.makeChoice(CHOICE_DOUBLE_DOWN_YES);
        DD_CONFIRM.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getDoubleDown()).isTrue();

        // Player busted: Ten+Eight+Five=23
        assertThat(game.getPlayerCards()).hasSize(3);
        assertThat(game.checkBust(game.getPlayerCards())).isTrue();

        // Dealer only revealed hidden card (one-card play on player bust): Ten + Nine = 2 cards
        assertThat(game.getDealerCards()).hasSize(2);

        // Player busted → player loses (ZERO_MULTI)
        assertThat(game.getHandMultiplier()).isEqualTo(ZERO_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void player11_dealer6_hit_playerReaches21_playerWins() {
        // Player hits (not doubling) to reach 21 → dealer plays out → bust → player wins
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, FIVE_RANK),
                Card.of(DIAMONDS_SUIT, SIX_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK),   // player hit → 21
                Card.of(HEARTS_SUIT, EIGHT_RANK) // dealer hit → 23 bust
        );

        assertThat(game.getAvailableChoices()).contains(CHOICE_HIT);

        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));

        // Player reached 21 → finalized immediately
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getPlayerCards()).hasSize(3);
        assertThat(game.playerHardScore()).isEqualTo(21);

        // Dealer played until soft 17: 6+9=15 → hits 8 → 23 bust
        assertThat(game.getDealerCards()).hasSize(3);
        assertThat(game.checkBust(game.getDealerCards())).isTrue();

        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void player11_dealer6_stand_dealerBusts_playerWins() {
        // Player stands on 11 → dealer 6+9=15 must hit → 8 → 23 bust → player wins
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, FIVE_RANK),
                Card.of(DIAMONDS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, EIGHT_RANK)  // dealer hits
        );

        game.makeChoice(CHOICE_STAND);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();

        // Dealer: 6+9=15, hits 8 → 23 bust
        assertThat(game.getDealerCards()).hasSize(3);
        assertThat(game.checkBust(game.getDealerCards())).isTrue();

        // Player 11 vs bust dealer → player wins
        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);

        // Stand recorded in takenChoices
        assertThat(game.getTakenChoices()).containsExactly(CHOICE_DEAL, CHOICE_STAND);
    }

    @Test
    void player11_dealer6_hit_playerHitsAgain_playerBusts_playerLoses() {
        // Player hits twice: 11+3=14, then +9=23 bust → player loses
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, FIVE_RANK),
                Card.of(DIAMONDS_SUIT, SIX_RANK),
                Card.of(CLUBS_SUIT, THREE_RANK),  // hit 1 → 14
                Card.of(HEARTS_SUIT, NINE_RANK),  // hit 2 → 23 bust
                Card.of(CLUBS_SUIT, ACE_RANK)     // dealer peek (1 card on player bust)
        );

        // First hit: 11+3=14, not bust → available: Stand + Hit
        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isFalse();
        assertThat(game.getPlayerCards()).hasSize(3);
        assertThat(game.playerHardScore()).isEqualTo(14);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_STAND, CHOICE_HIT, CHOICE_AUTO_FINALIZE);

        // Second hit: 14+9=23 bust
        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getPlayerCards()).hasSize(4);
        assertThat(game.checkBust(game.getPlayerCards())).isTrue();

        // Dealer only drew one card on player bust
        assertThat(game.getDealerCards()).hasSize(2);

        assertThat(game.getHandMultiplier()).isEqualTo(ZERO_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void player11_dealer6_surrender_losesHalf() {
        // Player surrenders on 11 vs 6 (allowed on first two cards)
        Game game = deal(
                Card.of(CLUBS_SUIT, SIX_RANK),
                Card.of(HEARTS_SUIT, NINE_RANK),
                Card.of(SPADES_SUIT, FIVE_RANK),
                Card.of(DIAMONDS_SUIT, SIX_RANK),
                Card.of(CLUBS_SUIT, ACE_RANK)  // dealer one-card peek
        );

        assertThat(game.getAvailableChoices()).contains(CHOICE_SURRENDER);

        game.makeChoice(CHOICE_SURRENDER);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(PT.surrenderMulti());
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    // =========================================================================
    // Scenario: PLAYER_6_DEALER_10_DD_NOT_BASIC
    //   Cards: dealer visible=Ten, dealer hidden=7 (total 17, stands)
    //          player=4+2=6 (double NOT recommended by basic strategy)
    //          player double card=Ten → 16
    // =========================================================================

    @Test
    void player6_dealer10_afterDeal_correctInitialState() {
        // BasicStrategy: player 6 ≤ 7 → getDoubleDown() returns false (not basic strategy)
        // After deal: choices include Surrender (dealer not Ace), Stand, Hit, Double; no Split (4≠2)
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),     // dealer visible
                Card.of(HEARTS_SUIT, SEVEN_RANK),   // dealer hidden
                Card.of(SPADES_SUIT, FOUR_RANK),    // player 0
                Card.of(DIAMONDS_SUIT, TWO_RANK),   // player 1
                Card.of(CLUBS_SUIT, TEN_RANK)       // double card (unused at deal time)
        );

        // Dealer: one visible card (Ten), hidden stored separately
        assertThat(game.getDealerCards()).hasSize(1);
        assertThat(game.getDealerCards().get(0).getRank()).isEqualTo(TEN_RANK);
        assertThat(game.getDealerSecondCard()).isNotNull();
        assertThat(game.getDealerSecondCard().getRank()).isEqualTo(SEVEN_RANK);

        // Player: 4+2=6
        assertThat(game.getPlayerCards()).hasSize(2);
        assertThat(game.getPlayerCards().get(0).getRank()).isEqualTo(FOUR_RANK);
        assertThat(game.getPlayerCards().get(1).getRank()).isEqualTo(TWO_RANK);
        assertThat(game.playerHardScore()).isEqualTo(6);
        assertThat(game.playerScore()).isEqualTo("6");

        // Not a pair (4 ≠ 2)
        assertThat(game.isPair()).isFalse();

        // Not finalized, no double, multiplier zero
        assertThat(game.getFinalized()).isFalse();
        assertThat(game.getDoubleDown()).isFalse();
        assertThat(game.getHandMultiplier()).isEqualTo(0.0);

        // Surrender available (dealer upcard not Ace, initial deal), no Split
        assertThat(game.getAvailableChoices())
                .containsExactlyInAnyOrder(CHOICE_SURRENDER, CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN, CHOICE_AUTO_PLAY);
        assertThat(game.getAvailableChoices()).doesNotContain(CHOICE_SPLIT);

        // takenChoices has only DEAL
        assertThat(game.getTakenChoices()).containsExactly(CHOICE_DEAL);

        // Verify BasicStrategy explicitly returns false for this hand
        assertThat(BASIC_STRATEGY.getDoubleDown(game)).isFalse();
    }

    @Test
    void player6_dealer10_double_notBasicStrategy_confirmModalShown() {
        // When player chooses Double on a non-basic-strategy hand, availableChoices=[YES, NO]
        // and takenChoices records CHOICE_DOUBLE_NOT_BASIC_STRATEGY.
        // We replicate what DoubleDownBetProcessor does in the false branch (no wallet calls).
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),
                Card.of(HEARTS_SUIT, SEVEN_RANK),
                Card.of(SPADES_SUIT, FOUR_RANK),
                Card.of(DIAMONDS_SUIT, TWO_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK)
        );

        game.makeChoice(CHOICE_DOUBLE_DOWN);
        applyNotBasicStrategyPrompt(game); // replicates DoubleDownBetProcessor false branch

        // Modal state: NOT finalized, doubleDown still false (wallet not committed yet)
        assertThat(game.getFinalized()).isFalse();
        assertThat(game.getDoubleDown()).isFalse();

        // Choices show exactly the YES/NO modal options
        assertThat(game.getAvailableChoices())
                .containsExactly(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO);

        // The not-basic-strategy choice was recorded
        assertThat(game.getTakenChoices())
                .containsExactly(CHOICE_DEAL, CHOICE_DOUBLE_DOWN, CHOICE_DOUBLE_NOT_BASIC_STRATEGY);

        // Cards unchanged — no hit yet
        assertThat(game.getPlayerCards()).hasSize(2);
        assertThat(game.getDealerCards()).hasSize(1);
    }

    @Test
    void player6_dealer10_double_notBasicStrategy_confirmYes_playerLoses() {
        // Player confirms YES on not-basic-strategy double.
        // DoubleDownYesWalletProcessor sets doubleDown=true (wallet already handled).
        // DoubleDownConfirmProcessor YES path: hits player, evaluates result.
        // Player: 4+2+Ten=16, dealer: Ten+7=17 → dealer stands, 17>16 → player loses (ZERO_MULTI)
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),
                Card.of(HEARTS_SUIT, SEVEN_RANK),
                Card.of(SPADES_SUIT, FOUR_RANK),
                Card.of(DIAMONDS_SUIT, TWO_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK)     // player double card → 16
        );

        // Simulate the full confirm flow without repo calls:
        // Step 1: DoubleDownBetProcessor false path
        game.makeChoice(CHOICE_DOUBLE_DOWN);
        applyNotBasicStrategyPrompt(game);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO);

        // Step 2: DoubleDownYesWalletProcessor sets doubleDown=true
        applyWalletDoubleState(game);

        // Step 3: DoubleDownConfirmProcessor handles YES (hits player, evaluates result)
        game.makeChoice(CHOICE_DOUBLE_DOWN_YES);
        DD_CONFIRM.process(ctx(game));

        // Hand finalized
        assertThat(game.getFinalized()).isTrue();
        assertThat(game.getDoubleDown()).isTrue();

        // Player: 4+2+Ten = 16 (3 cards)
        assertThat(game.getPlayerCards()).hasSize(3);
        assertThat(game.getPlayerCards().get(2).getRank()).isEqualTo(TEN_RANK);
        assertThat(game.playerHardScore()).isEqualTo(16);

        // Dealer: Ten+7 = 17 (stands immediately after revealing hidden card, 2 cards total)
        assertThat(game.getDealerCards()).hasSize(2);
        assertThat(game.getDealerCards().get(0).getRank()).isEqualTo(TEN_RANK);
        assertThat(game.getDealerCards().get(1).getRank()).isEqualTo(SEVEN_RANK);
        assertThat(game.checkBust(game.getDealerCards())).isFalse();

        // Dealer 17 beats player 16 → player loses
        assertThat(game.getHandMultiplier()).isEqualTo(ZERO_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);

        // takenChoices: DEAL → DOUBLE_DOWN → DOUBLE_NOT_BASIC_STRATEGY → DOUBLE_DOWN_YES
        assertThat(game.getTakenChoices()).containsExactly(
                CHOICE_DEAL, CHOICE_DOUBLE_DOWN, CHOICE_DOUBLE_NOT_BASIC_STRATEGY, CHOICE_DOUBLE_DOWN_YES);
    }

    @Test
    void player6_dealer10_double_notBasicStrategy_confirmNo_normalChoicesRestored() {
        // Player cancels the confirmation (NO).
        // DoubleDownConfirmProcessor NO path restores normal post-deal choices.
        // Surrender is gone because dealerCards.size()==1 after adjustDealerCardsAfterDeal
        // (INITIAL_DEALT_CARD_COUNT==2, so the Surrender guard fails).
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),
                Card.of(HEARTS_SUIT, SEVEN_RANK),
                Card.of(SPADES_SUIT, FOUR_RANK),
                Card.of(DIAMONDS_SUIT, TWO_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK)
        );

        // Step 1: trigger confirm modal state
        game.makeChoice(CHOICE_DOUBLE_DOWN);
        applyNotBasicStrategyPrompt(game);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO);

        // Step 2: player chooses NO — DoubleDownConfirmProcessor NO path is wallet-free
        game.makeChoice(CHOICE_DOUBLE_DOWN_NO);
        CHAIN.process(ctx(game));

        // Modal dismissed — not finalized, doubleDown still false
        assertThat(game.getFinalized()).isFalse();
        assertThat(game.getDoubleDown()).isFalse();

        // Normal play choices restored. Surrender is available — dealer has 1 visible card (10),
        // DoubleDownConfirmProcessor restores the initial-deal choice set when dealerCards.size()==1.
        assertThat(game.getAvailableChoices())
                .containsExactlyInAnyOrder(CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN, CHOICE_AUTO_FINALIZE, CHOICE_SURRENDER);
        assertThat(game.getAvailableChoices()).doesNotContain(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO);

        // Cards unchanged — no hit
        assertThat(game.getPlayerCards()).hasSize(2);
        assertThat(game.getDealerCards()).hasSize(1);

        // takenChoices: DEAL → DOUBLE_DOWN → DOUBLE_NOT_BASIC_STRATEGY → DOUBLE_DOWN_NO
        assertThat(game.getTakenChoices()).containsExactly(
                CHOICE_DEAL, CHOICE_DOUBLE_DOWN, CHOICE_DOUBLE_NOT_BASIC_STRATEGY, CHOICE_DOUBLE_DOWN_NO);
    }

    @Test
    void player6_dealer10_double_notBasicStrategy_confirmNo_thenStand_dealerWins() {
        // After cancelling the double, player stands → dealer 17 beats player 6 → player loses
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),
                Card.of(HEARTS_SUIT, SEVEN_RANK),
                Card.of(SPADES_SUIT, FOUR_RANK),
                Card.of(DIAMONDS_SUIT, TWO_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK)
        );

        game.makeChoice(CHOICE_DOUBLE_DOWN);
        applyNotBasicStrategyPrompt(game);
        game.makeChoice(CHOICE_DOUBLE_DOWN_NO);
        CHAIN.process(ctx(game));
        assertThat(game.getAvailableChoices()).contains(CHOICE_STAND);

        // Stand: dealer Ten+7=17 stands, player 6, dealer wins
        game.makeChoice(CHOICE_STAND);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();
        // Dealer 17 > player 6 → player loses
        assertThat(game.getHandMultiplier()).isEqualTo(ZERO_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }

    @Test
    void player6_dealer10_double_notBasicStrategy_confirmNo_thenHit_playerContinues() {
        // After cancelling double, player hits: 4+2+Ten=16 → not bust → Stand + Hit offered
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),
                Card.of(HEARTS_SUIT, SEVEN_RANK),
                Card.of(SPADES_SUIT, FOUR_RANK),
                Card.of(DIAMONDS_SUIT, TWO_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK)  // player hit → 16
        );

        game.makeChoice(CHOICE_DOUBLE_DOWN);
        applyNotBasicStrategyPrompt(game);
        game.makeChoice(CHOICE_DOUBLE_DOWN_NO);
        CHAIN.process(ctx(game));

        // Hit: 6+Ten=16
        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isFalse();
        assertThat(game.getPlayerCards()).hasSize(3);
        assertThat(game.playerHardScore()).isEqualTo(16);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_STAND, CHOICE_HIT, CHOICE_AUTO_FINALIZE);
    }

    @Test
    void player6_dealer10_hit_playerBusts_playerLoses() {
        // Player hits directly: 6+Ten=16, hits again → 16+Nine=25 bust → player loses
        Game game = deal(
                Card.of(CLUBS_SUIT, TEN_RANK),
                Card.of(HEARTS_SUIT, SEVEN_RANK),
                Card.of(SPADES_SUIT, FOUR_RANK),
                Card.of(DIAMONDS_SUIT, TWO_RANK),
                Card.of(CLUBS_SUIT, TEN_RANK),   // hit 1 → 16
                Card.of(HEARTS_SUIT, NINE_RANK),  // hit 2 → 25 bust
                Card.of(CLUBS_SUIT, ACE_RANK)     // dealer peek
        );

        // First hit → 16
        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));
        assertThat(game.getFinalized()).isFalse();
        assertThat(game.playerHardScore()).isEqualTo(16);

        // Second hit → bust
        game.makeChoice(CHOICE_HIT);
        CHAIN.process(ctx(game));

        assertThat(game.getFinalized()).isTrue();
        assertThat(game.checkBust(game.getPlayerCards())).isTrue();
        assertThat(game.getHandMultiplier()).isEqualTo(ZERO_MULTI);
        assertThat(game.getAvailableChoices()).containsExactly(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL);
    }
}
