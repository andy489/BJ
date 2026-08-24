package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.config.PaytableProperties;
import com.casino.blackjack.service.gamelogic.dto.Card;
import org.junit.jupiter.api.Test;

import static com.casino.blackjack.service.gamelogic.SideBetEvaluator.*;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — no Spring context, no database, no RNG.
 *
 * Perfect Pairs: evaluated on first two player cards.
 *   Mixed Pair  = same rank, different suits, different colours   → 8.0
 *   Coloured    = same rank, different suits, same colour         → 13.0
 *   Perfect     = same rank, same suit                           → 26.0
 *
 * 21+3: evaluated on player[0], player[1], dealer up-card.
 *   Flush              = same suit, no rank pattern              → 6.0
 *   Straight           = consecutive ranks, any suits            → 11.0
 *   Three-of-a-Kind    = same rank, any suits                    → 31.0
 *   Straight Flush     = same suit + consecutive ranks           → 41.0
 *   Suited Three-of-Kind = same suit + same rank                 → 101.0
 */
class SideBetEvaluatorTest {

    private static final PaytableProperties PT = new PaytableProperties();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Card card(int suit, int rank) {
        return Card.of(suit, rank);
    }

    // ══ Perfect Pairs ════════════════════════════════════════════════════════

    @Test
    void pp_noMatch_differentRanks() {
        Card c0 = card(CLUBS_SUIT, ACE_RANK);
        Card c1 = card(CLUBS_SUIT, TWO_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(0.0);
    }

    @Test
    void pp_perfect_sameRankSameSuit() {
        Card c0 = card(HEARTS_SUIT, SEVEN_RANK);
        Card c1 = card(HEARTS_SUIT, SEVEN_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(PT.ppPerfectMulti());
    }

    @Test
    void pp_coloured_sameRankSameColourDifferentSuit_redRed() {
        // Hearts (red) vs Diamonds (red)
        Card c0 = card(HEARTS_SUIT, KING_RANK);
        Card c1 = card(DIAMONDS_SUIT, KING_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(PT.ppColouredMulti());
    }

    @Test
    void pp_coloured_sameRankSameColourDifferentSuit_blackBlack() {
        // Clubs (black) vs Spades (black)
        Card c0 = card(CLUBS_SUIT, TEN_RANK);
        Card c1 = card(SPADES_SUIT, TEN_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(PT.ppColouredMulti());
    }

    @Test
    void pp_mixed_sameRankDifferentColour() {
        // Clubs (black) vs Hearts (red)
        Card c0 = card(CLUBS_SUIT, ACE_RANK);
        Card c1 = card(HEARTS_SUIT, ACE_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(PT.ppMixedMulti());
    }

    @Test
    void pp_mixed_sameRankSpadesDiamonds() {
        // Spades (black) vs Diamonds (red)
        Card c0 = card(SPADES_SUIT, QUEEN_RANK);
        Card c1 = card(DIAMONDS_SUIT, QUEEN_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(PT.ppMixedMulti());
    }

    @Test
    void pp_tenAndJackAreNotAPair() {
        // 10 ≠ J — strict rank equality (not 10-value grouping)
        Card c0 = card(CLUBS_SUIT, TEN_RANK);
        Card c1 = card(CLUBS_SUIT, JAKE_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(0.0);
    }

    @Test
    void pp_jackAndQueenAreNotAPair() {
        Card c0 = card(SPADES_SUIT, JAKE_RANK);
        Card c1 = card(SPADES_SUIT, QUEEN_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(0.0);
    }

    @Test
    void pp_perfectPairAces() {
        Card c0 = card(SPADES_SUIT, ACE_RANK);
        Card c1 = card(SPADES_SUIT, ACE_RANK);
        assertThat(evalPerfectPairs(c0, c1, PT)).isEqualTo(PT.ppPerfectMulti());
    }

    // ══ 21+3 ════════════════════════════════════════════════════════════════

    @Test
    void t3_noWin_unrelatedCards() {
        // 2 of clubs, 5 of diamonds, 9 of hearts — no flush, straight, or set
        Card p0 = card(CLUBS_SUIT, TWO_RANK);
        Card p1 = card(DIAMONDS_SUIT, FIVE_RANK);
        Card d  = card(HEARTS_SUIT, NINE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(0.0);
    }

    @Test
    void t3_flush_sameSuitNoOtherPattern() {
        // 2♠ 5♠ 9♠ — same suit, ranks not consecutive, not all equal
        Card p0 = card(SPADES_SUIT, TWO_RANK);
        Card p1 = card(SPADES_SUIT, FIVE_RANK);
        Card d  = card(SPADES_SUIT, NINE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3FlushMulti());
    }

    @Test
    void t3_straight_consecutiveRanksAnyOrder() {
        // 7♣ 9♦ 8♥ — ranks 7,8,9 sequential, mixed suits
        Card p0 = card(CLUBS_SUIT, SEVEN_RANK);
        Card p1 = card(DIAMONDS_SUIT, NINE_RANK);
        Card d  = card(HEARTS_SUIT, EIGHT_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightMulti());
    }

    @Test
    void t3_straight_aceLow_A_2_3() {
        // A♣ 2♦ 3♥ — ace-low straight (ranks 1,2,3)
        Card p0 = card(CLUBS_SUIT, ACE_RANK);
        Card p1 = card(DIAMONDS_SUIT, TWO_RANK);
        Card d  = card(HEARTS_SUIT, THREE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightMulti());
    }

    @Test
    void t3_straight_tenJackQueen() {
        // 10♣ J♦ Q♥ — ranks 10,11,12 sequential (Broadway high-end)
        Card p0 = card(CLUBS_SUIT, TEN_RANK);
        Card p1 = card(DIAMONDS_SUIT, JAKE_RANK);
        Card d  = card(HEARTS_SUIT, QUEEN_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightMulti());
    }

    @Test
    void t3_straight_jackQueenKing() {
        // J♠ Q♣ K♦ — ranks 11,12,13
        Card p0 = card(SPADES_SUIT, JAKE_RANK);
        Card p1 = card(CLUBS_SUIT, QUEEN_RANK);
        Card d  = card(DIAMONDS_SUIT, KING_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightMulti());
    }

    @Test
    void t3_threeOfAKind_sameRankDifferentSuits() {
        // 7♣ 7♦ 7♥ — same rank, all different suits (not same suit → not suited 3K)
        Card p0 = card(CLUBS_SUIT, SEVEN_RANK);
        Card p1 = card(DIAMONDS_SUIT, SEVEN_RANK);
        Card d  = card(HEARTS_SUIT, SEVEN_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3ThreeOfAKindMulti());
    }

    @Test
    void t3_threeOfAKind_sameRankTwoSuitsMatch() {
        // A♣ A♣ A♦ — two share suit but not all three, still Three-of-a-Kind
        Card p0 = card(CLUBS_SUIT, ACE_RANK);
        Card p1 = card(CLUBS_SUIT, ACE_RANK);
        Card d  = card(DIAMONDS_SUIT, ACE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3ThreeOfAKindMulti());
    }

    @Test
    void t3_straightFlush_sameSuitConsecutiveRanks() {
        // 8♥ 9♥ 10♥ — same suit + consecutive (straight flush)
        Card p0 = card(HEARTS_SUIT, EIGHT_RANK);
        Card p1 = card(HEARTS_SUIT, NINE_RANK);
        Card d  = card(HEARTS_SUIT, TEN_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightFlushMulti());
    }

    @Test
    void t3_straightFlush_aceLow() {
        // A♦ 2♦ 3♦
        Card p0 = card(DIAMONDS_SUIT, ACE_RANK);
        Card p1 = card(DIAMONDS_SUIT, TWO_RANK);
        Card d  = card(DIAMONDS_SUIT, THREE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightFlushMulti());
    }

    @Test
    void t3_straightFlush_jackQueenKing() {
        // J♠ Q♠ K♠
        Card p0 = card(SPADES_SUIT, JAKE_RANK);
        Card p1 = card(SPADES_SUIT, QUEEN_RANK);
        Card d  = card(SPADES_SUIT, KING_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightFlushMulti());
    }

    @Test
    void t3_suitedThreeOfAKind_sameSuitSameRank() {
        // 7♠ 7♠ 7♠ — highest possible
        Card p0 = card(SPADES_SUIT, SEVEN_RANK);
        Card p1 = card(SPADES_SUIT, SEVEN_RANK);
        Card d  = card(SPADES_SUIT, SEVEN_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3SuitedThreeMulti());
    }

    @Test
    void t3_suitedThreeOfAKind_aces() {
        // A♦ A♦ A♦
        Card p0 = card(DIAMONDS_SUIT, ACE_RANK);
        Card p1 = card(DIAMONDS_SUIT, ACE_RANK);
        Card d  = card(DIAMONDS_SUIT, ACE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3SuitedThreeMulti());
    }

    @Test
    void t3_tenJackQueenStraight_isNotThreeOfAKind() {
        // Different ranks even if all 10-value: 10, J, Q are distinct
        Card p0 = card(CLUBS_SUIT, TEN_RANK);
        Card p1 = card(CLUBS_SUIT, JAKE_RANK);
        Card d  = card(CLUBS_SUIT, QUEEN_RANK);
        // Same suit + consecutive = Straight Flush (not 3K)
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightFlushMulti());
    }

    @Test
    void t3_tenKingQueenIsNotAStraight() {
        // 10, Q, K — skip of 1 between 10 and Q: not consecutive
        Card p0 = card(CLUBS_SUIT, TEN_RANK);
        Card p1 = card(DIAMONDS_SUIT, QUEEN_RANK);
        Card d  = card(HEARTS_SUIT, KING_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(0.0);
    }

    @Test
    void t3_nineKingQueenIsNotAStraight() {
        Card p0 = card(CLUBS_SUIT, NINE_RANK);
        Card p1 = card(DIAMONDS_SUIT, QUEEN_RANK);
        Card d  = card(HEARTS_SUIT, KING_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(0.0);
    }

    @Test
    void t3_rankPrecedence_suitedThreeBeatsAll() {
        // Ensure suited 3-of-a-kind is not accidentally classified as straight-flush or flush
        Card p0 = card(HEARTS_SUIT, NINE_RANK);
        Card p1 = card(HEARTS_SUIT, NINE_RANK);
        Card d  = card(HEARTS_SUIT, NINE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3SuitedThreeMulti());
    }

    @Test
    void t3_straightFlushBeats3K() {
        // 3♥ 4♥ 5♥ — same suit, consecutive: straight flush beats plain 3K
        // (ranks are different so 3K doesn't apply, but priority test still valid)
        Card p0 = card(HEARTS_SUIT, THREE_RANK);
        Card p1 = card(HEARTS_SUIT, FOUR_RANK);
        Card d  = card(HEARTS_SUIT, FIVE_RANK);
        assertThat(eval21_3(p0, p1, d, PT)).isEqualTo(PT.t3StraightFlushMulti());
    }
}
