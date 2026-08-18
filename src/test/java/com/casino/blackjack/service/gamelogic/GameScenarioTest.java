package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.processor.GameContext;
import com.casino.blackjack.service.gamelogic.processor.GameStateProcessorChain;
import com.casino.blackjack.service.gamelogic.rng.FixedCardSource;
import org.junit.jupiter.api.Test;

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
 */
class GameScenarioTest {

    private static final GameStateProcessorChain CHAIN = new GameStateProcessorChain();

    /** Minimal context for pure-logic tests — all wallet/repo fields are null (unused by pure processors). */
    private static GameContext ctx(Game game) {
        return new GameContext(game, null, null, null, null, null, null, null, null, null);
    }

    /** Convenience: build a freshly-dealt game with fixed cards and run the processor chain. */
    private Game deal(Card... cards) {
        Game game = new Game();
        game.setCardSource(new FixedCardSource(cards));
        game.setDealt(true);
        game.deal();
        game.makeChoice(CHOICE_DEAL);
        CHAIN.process(ctx(game));
        game.adjustDealerCardsAfterDeal();
        return game;
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
        assertThat(game.getHandMultiplier()).isEqualTo(BJ_MULTI);
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
        assertThat(game.getHandMultiplier()).isEqualTo(BJ_MULTI);
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
}
