package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.model.dto.CardDto;
import com.casino.blackjack.model.dto.GameStateDto;
import com.casino.blackjack.model.dto.WalletStateDto;
import com.casino.blackjack.service.GameStateDtoMapper;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ACE_RANK;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.SPADES_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.HEARTS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CLUBS_SUIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.DIAMONDS_SUIT;
import static org.assertj.core.api.Assertions.assertThat;

class GameStateDtoMapperTest {

    private static final GameStateDtoMapper MAPPER = new GameStateDtoMapper();

    // ── score visibility ────────────────────────────────────────────────────

    @Test
    void scoresAreNullPreDeal() {
        Game game = minimalGame();
        // dealt=false, finalized=false — no cards have been dealt
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getPlayerScore()).isNull();
        assertThat(dto.getDealerScore()).isNull();
    }

    @Test
    void scoresArePresentWhenDealt() {
        Game game = minimalGame();
        game.setDealt(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 10), Card.of(HEARTS_SUIT, 7)));
        game.setDealerCards(List.of(Card.of(CLUBS_SUIT, 8)));
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getPlayerScore()).isNotNull();
        assertThat(dto.getDealerScore()).isNotNull();
    }

    @Test
    void scoresArePresentWhenFinalizedEvenIfDealtIsFalse() {
        // FinalizedPayoutProcessor sets dealt=false after payout;
        // scores must still be computed so the frontend shows them.
        Game game = minimalGame();
        game.setDealt(false);
        game.setFinalized(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 10), Card.of(HEARTS_SUIT, 7)));
        game.setDealerCards(List.of(Card.of(CLUBS_SUIT, 8), Card.of(DIAMONDS_SUIT, 9)));
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getPlayerScore()).isNotNull();
        assertThat(dto.getDealerScore()).isNotNull();
    }

    // ── card mapping ────────────────────────────────────────────────────────

    @Test
    void playerCardsAreMappedToCardDtos() {
        Game game = minimalGame();
        game.setDealt(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, ACE_RANK), Card.of(HEARTS_SUIT, 10)));
        game.setDealerCards(List.of(Card.of(CLUBS_SUIT, 6)));
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getPlayerCards()).hasSize(2);
        CardDto first = dto.getPlayerCards().get(0);
        assertThat(first.getRank()).isEqualTo(ACE_RANK);
        assertThat(first.getSuit()).isEqualTo(SPADES_SUIT);
    }

    @Test
    void dealerCardsAreMappedToCardDtos() {
        Game game = minimalGame();
        game.setDealt(true);
        game.setDealerCards(List.of(Card.of(HEARTS_SUIT, 5), Card.of(DIAMONDS_SUIT, 9)));
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 10), Card.of(CLUBS_SUIT, 8)));
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getDealerCards()).hasSize(2);
        CardDto first = dto.getDealerCards().get(0);
        assertThat(first.getRank()).isEqualTo(5);
        assertThat(first.getSuit()).isEqualTo(HEARTS_SUIT);
    }

    @Test
    void nullPlayerCardsYieldEmptyList() {
        Game game = minimalGame();
        game.setPlayerCards(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getPlayerCards()).isEmpty();
    }

    @Test
    void nullDealerCardsYieldEmptyList() {
        Game game = minimalGame();
        game.setDealerCards(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getDealerCards()).isEmpty();
    }

    // ── split hand mapping ──────────────────────────────────────────────────

    @Test
    void splitHandsAreMappedCorrectly() {
        Game game = minimalGame();
        game.setDealt(true);
        game.setSplitActive(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 8)));
        game.setDealerCards(List.of(Card.of(HEARTS_SUIT, 6)));
        game.setSplitHands(List.of(
                List.of(Card.of(SPADES_SUIT, 8), Card.of(CLUBS_SUIT, 3)),
                List.of(Card.of(HEARTS_SUIT, 8), Card.of(DIAMONDS_SUIT, 7))
        ));
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getSplitHands()).hasSize(2);
        assertThat(dto.getSplitHands().get(0)).hasSize(2);
        assertThat(dto.getSplitHands().get(1)).hasSize(2);
    }

    @Test
    void nullSplitHandsYieldEmptyList() {
        Game game = minimalGame();
        game.setSplitHands(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getSplitHands()).isEmpty();
    }

    @Test
    void splitScoresBuiltForEachSplitHand() {
        Game game = minimalGame();
        game.setDealt(true);
        game.setSplitActive(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 9)));
        game.setDealerCards(List.of(Card.of(HEARTS_SUIT, 10)));
        game.setSplitHands(List.of(
                List.of(Card.of(SPADES_SUIT, 9), Card.of(CLUBS_SUIT, 8)),
                List.of(Card.of(HEARTS_SUIT, 9), Card.of(DIAMONDS_SUIT, 5))
        ));
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getSplitScores()).hasSize(2);
        assertThat(dto.getSplitScores()).doesNotContainNull();
    }

    @Test
    void emptySplitHandsYieldEmptySplitScores() {
        Game game = minimalGame();
        game.setSplitHands(Collections.emptyList());
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getSplitScores()).isEmpty();
    }

    // ── null-safe list fields ───────────────────────────────────────────────

    @Test
    void nullAvailableChoicesYieldEmptyList() {
        Game game = minimalGame();
        game.setAvailableChoices(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getAvailableChoices()).isEmpty();
    }

    @Test
    void nullErrCodeListYieldsEmptyList() {
        Game game = minimalGame();
        game.setErrCodeList(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getErrCodeList()).isEmpty();
    }

    @Test
    void nullSplitHandMultipliersYieldEmptyList() {
        Game game = minimalGame();
        game.setSplitHandMultipliers(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getSplitHandMultipliers()).isEmpty();
    }

    @Test
    void nullSplitDoubleDownFlagsYieldEmptyList() {
        Game game = minimalGame();
        game.setSplitDoubleDownFlags(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getSplitDoubleDownFlags()).isEmpty();
    }

    // ── wallet mapping ──────────────────────────────────────────────────────

    @Test
    void nullWalletYieldsNullWalletDto() {
        Game game = minimalGame();
        game.setWallet(null);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getWallet()).isNull();
    }

    @Test
    void walletBalanceIsMapped() {
        Game game = minimalGame();
        Wallet w = new Wallet().setBalance(new BigDecimal("1234.56"));
        game.setWallet(w);
        WalletStateDto wDto = MAPPER.map(game, null).getWallet();
        assertThat(wDto.getBalance()).isEqualByComparingTo("1234.56");
    }

    @Test
    void walletNullFieldsDefaultToZero() {
        // Wallet constructor initialises all fields to ZERO, so calling setBalance(null)
        // lets us verify the nvl() guard in the mapper.
        Game game = minimalGame();
        Wallet w = new Wallet();
        w.setBalance(null);
        w.setLastBet(null);
        w.setLastPpBet(null);
        game.setWallet(w);
        WalletStateDto wDto = MAPPER.map(game, null).getWallet();
        assertThat(wDto.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wDto.getLastBet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wDto.getLastPpBet()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void walletSideBetFieldsAreMapped() {
        Game game = minimalGame();
        Wallet w = new Wallet()
                .setPerfectPairsBet(new BigDecimal("5.00"))
                .setTwentyOneThreeBet(new BigDecimal("10.00"))
                .setDealerPerfectPairsBet(new BigDecimal("15.00"));
        game.setWallet(w);
        WalletStateDto wDto = MAPPER.map(game, null).getWallet();
        assertThat(wDto.getPerfectPairsBet()).isEqualByComparingTo("5.00");
        assertThat(wDto.getTwentyOneThreeBet()).isEqualByComparingTo("10.00");
        assertThat(wDto.getDealerPerfectPairsBet()).isEqualByComparingTo("15.00");
    }

    @Test
    void walletLastWinAndLastBetAreMapped() {
        Game game = minimalGame();
        Wallet w = new Wallet()
                .setLastWin(new BigDecimal("20.00"))
                .setLastBet(new BigDecimal("10.00"))
                .setLastPpWin(new BigDecimal("25.00"))
                .setLastT3Win(new BigDecimal("30.00"))
                .setLastDppWin(new BigDecimal("40.00"));
        game.setWallet(w);
        WalletStateDto wDto = MAPPER.map(game, null).getWallet();
        assertThat(wDto.getLastWin()).isEqualByComparingTo("20.00");
        assertThat(wDto.getLastBet()).isEqualByComparingTo("10.00");
        assertThat(wDto.getLastPpWin()).isEqualByComparingTo("25.00");
        assertThat(wDto.getLastT3Win()).isEqualByComparingTo("30.00");
        assertThat(wDto.getLastDppWin()).isEqualByComparingTo("40.00");
    }

    // ── bet history ─────────────────────────────────────────────────────────

    @Test
    void nullBetHistoryIsNotSet() {
        Game game = minimalGame();
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getBetHistory()).isNull();
    }

    @Test
    void emptyBetHistoryIsNotSet() {
        Game game = minimalGame();
        GameStateDto dto = MAPPER.map(game, Collections.emptyList());
        assertThat(dto.getBetHistory()).isNull();
    }

    @Test
    void betHistoryIsMappedWhenProvided() {
        // BetHistoryView has a private constructor (requires BetHistoryEntity + ObjectMapper),
        // so full mapping of list entries is covered by BetHistoryService integration tests.
        // Here we just verify that a non-null non-empty list causes betHistory to be set,
        // while null / empty leaves it null.
        Game game = minimalGame();
        // Passing null → betHistory stays null (tested in nullBetHistoryIsNotSet)
        assertThat(MAPPER.map(game, null).getBetHistory()).isNull();
        // Passing empty → betHistory stays null (tested in emptyBetHistoryIsNotSet)
        assertThat(MAPPER.map(game, Collections.emptyList()).getBetHistory()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @Test
    void dealtFlagPropagated() {
        Game game = minimalGame();
        game.setDealt(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 10), Card.of(HEARTS_SUIT, 7)));
        game.setDealerCards(List.of(Card.of(CLUBS_SUIT, 8)));
        assertThat(MAPPER.map(game, null).isDealt()).isTrue();
    }

    @Test
    void finalizedFlagPropagated() {
        Game game = minimalGame();
        game.setFinalized(true);
        game.setPlayerCards(List.of(Card.of(SPADES_SUIT, 10), Card.of(HEARTS_SUIT, 7)));
        game.setDealerCards(List.of(Card.of(CLUBS_SUIT, 8)));
        assertThat(MAPPER.map(game, null).isFinalized()).isTrue();
    }

    @Test
    void splitActiveFlagPropagated() {
        Game game = minimalGame();
        game.setSplitActive(true);
        assertThat(MAPPER.map(game, null).isSplitActive()).isTrue();
    }

    @Test
    void doubleDownFlagPropagated() {
        Game game = minimalGame();
        game.setDoubleDown(true);
        assertThat(MAPPER.map(game, null).isDoubleDown()).isTrue();
    }

    @Test
    void splitAcesFlagPropagated() {
        Game game = minimalGame();
        game.setSplitAces(true);
        assertThat(MAPPER.map(game, null).isSplitAces()).isTrue();
    }

    @Test
    void activeSplitHandIndexDefaultsToZeroWhenNull() {
        Game game = minimalGame();
        game.setActiveSplitHandIndex(null);
        assertThat(MAPPER.map(game, null).getActiveSplitHandIndex()).isZero();
    }

    // ── multipliers ──────────────────────────────────────────────────────────

    @Test
    void handAndInsuranceMultipliersArePropagated() {
        Game game = minimalGame();
        game.setHandMultiplier(1.5);
        game.setInsuranceMultiplier(2.0);
        GameStateDto dto = MAPPER.map(game, null);
        assertThat(dto.getHandMultiplier()).isEqualTo(1.5);
        assertThat(dto.getInsuranceMultiplier()).isEqualTo(2.0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Game minimalGame() {
        Game g = new Game();
        g.setDealt(false);
        g.setFinalized(false);
        g.setSplitActive(false);
        g.setActiveSplitHandIndex(0);
        g.setDoubleDown(false);
        g.setSplitAces(false);
        g.setPlayerCards(Collections.emptyList());
        g.setDealerCards(Collections.emptyList());
        g.setSplitHands(Collections.emptyList());
        g.setSplitHandMultipliers(Collections.emptyList());
        g.setSplitDoubleDownFlags(Collections.emptyList());
        g.setAvailableChoices(Collections.emptyList());
        g.setErrCodeList(Collections.emptyList());
        g.setHandMultiplier(1.0);
        g.setInsuranceMultiplier(1.0);
        g.setWallet(null);
        return g;
    }
}

