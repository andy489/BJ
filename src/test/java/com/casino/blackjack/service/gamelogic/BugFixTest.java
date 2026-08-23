package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.processor.GameContext;
import com.casino.blackjack.service.gamelogic.processor.InsuranceBetProcessor;
import com.casino.blackjack.service.gamelogic.processor.SideBetPlacementProcessor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the three bug fixes:
 *
 * Bug 1 — WalletEntity.payBet() must store handBet (not currentBet) into lastBet.
 * Bug 2 — SideBetPlacementProcessor must reject side bets after cards are dealt.
 * Bug 3 — InsuranceBetProcessor must not deduct insurance a second time when already placed.
 */
class BugFixTest {

    private static BigDecimal bd(double v) {
        return new BigDecimal(String.valueOf(v));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bug 1 — WalletEntity.payBet() lastBet must equal handBet, not currentBet
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player bets £50 (handBet=50, currentBet=50), then doubles down so currentBet becomes £100.
     * After payBet() the lastBet must be £50 (the original hand bet), not £100.
     */
    @Test
    void payBet_afterDoubleDown_lastBetEqualsHandBetNotCurrentBet() {
        WalletEntity wallet = new WalletEntity();
        wallet.setBalance(bd(1000));

        // Place initial hand bet of £50
        wallet.placeHandBet(bd(50));
        assertThat(wallet.getHandBet()).isEqualByComparingTo(bd(50));
        assertThat(wallet.getCurrentBet()).isEqualByComparingTo(bd(50));

        // Simulate double-down: currentBet doubles, doubleBet = handBet
        wallet.setCurrentBet(bd(100));
        wallet.setDoubleBet(bd(50));
        wallet.setBalance(wallet.getBalance().subtract(bd(50))); // balance after doubling

        // Pay out (player wins at 2x)
        wallet.payBet(DOUBLE_MULTI, 0.0);

        // lastBet must record the original hand bet (£50), not the doubled currentBet (£100)
        assertThat(wallet.getLastBet()).isEqualByComparingTo(bd(50));
    }

    /**
     * Sanity check: without a double-down, handBet == currentBet, so lastBet is the same either way.
     */
    @Test
    void payBet_withoutDoubleDown_lastBetEqualsHandBet() {
        WalletEntity wallet = new WalletEntity();
        wallet.setBalance(bd(500));
        wallet.placeHandBet(bd(75));

        wallet.payBet(DOUBLE_MULTI, 0.0);

        assertThat(wallet.getLastBet()).isEqualByComparingTo(bd(75));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bug 2 — SideBetPlacementProcessor must not fire after cards are dealt
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When initialPlayerCards is set on the GameEntity (hand already dealt),
     * canProcess() must return false so the side bet is rejected.
     */
    @Test
    void sideBetPlacementProcessor_rejectsPlacementAfterDeal() {
        SideBetPlacementProcessor processor = new SideBetPlacementProcessor();

        // Build a Game that requests a PP side bet
        Game game = new Game().makeChoice(CHOICE_PLACE_PERFECT_PAIRS);
        game.setSideBetAmountStr("5");

        // Build a GameEntity that simulates a dealt hand (initialPlayerCards is non-null)
        GameEntity gameEntity = new GameEntity();
        gameEntity.setFinalized(false);
        gameEntity.setInitialPlayerCards("[{\"rank\":5,\"suit\":0},{\"rank\":6,\"suit\":1}]");

        WalletEntity wallet = new WalletEntity();
        wallet.setBalance(bd(200));
        wallet.setHandBet(bd(25));

        GameContext ctx = new GameContext(
                game, gameEntity, wallet,
                null, null, null, null, null, null, null,
                4, 3000
        );

        assertThat(processor.canProcess(ctx)).isFalse();
        // Balance must be unchanged — the processor never ran
        assertThat(wallet.getBalance()).isEqualByComparingTo(bd(200));
        assertThat(wallet.getPerfectPairsBet()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Before the deal (initialPlayerCards is null), canProcess() returns true for side-bet choices.
     */
    @Test
    void sideBetPlacementProcessor_allowsPlacementBeforeDeal() {
        SideBetPlacementProcessor processor = new SideBetPlacementProcessor();

        Game game = new Game().makeChoice(CHOICE_PLACE_PERFECT_PAIRS);
        game.setSideBetAmountStr("5");

        GameEntity gameEntity = new GameEntity();
        gameEntity.setFinalized(false);
        // initialPlayerCards is null — hand not yet dealt

        WalletEntity wallet = new WalletEntity();
        wallet.setBalance(bd(200));

        GameContext ctx = new GameContext(
                game, gameEntity, wallet,
                null, null, null, null, null, null, null,
                4, 3000
        );

        assertThat(processor.canProcess(ctx)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bug 3 — InsuranceBetProcessor must not deduct insurance twice
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calling InsuranceBetProcessor.process() a second time when insurance is already placed
     * must be a no-op: wallet balance stays the same as after the first call.
     *
     * We simulate the state AFTER the first insurance placement (insurance=true, balance already
     * reduced) and verify that submitting CHOICE_INSURANCE_YES again does not touch the wallet.
     * The guard fires before any repo calls, so passing null repos proves it returned early.
     */
    @Test
    void insuranceBetProcessor_secondCallIsNoOp_whenInsuranceAlreadyPlaced() {
        InsuranceBetProcessor processor = new InsuranceBetProcessor();

        // Wallet state AFTER the first insurance deduction:
        //   original balance £200, hand bet £50, insurance (halfBet) £25 already deducted.
        WalletEntity wallet = new WalletEntity();
        wallet.setBalance(bd(125)); // 200 - 50 (handBet) - 25 (insuranceBet already taken)
        wallet.setHandBet(bd(50));
        wallet.setCurrentBet(bd(75)); // 50 hand + 25 insurance
        wallet.setInsuranceBet(bd(25));

        // Game already has insurance=true (first call succeeded)
        Game game = new Game().makeChoice(CHOICE_DEAL).makeChoice(CHOICE_INSURANCE_YES);
        game.setInsurance(true);

        GameContext ctx = new GameContext(
                game, null, wallet,
                null, null, null, null, null, null, null,
                4, 3000
        );

        // Second call must be a no-op (guard returns before touching wallet or repos)
        processor.process(ctx);

        // Balance unchanged
        assertThat(wallet.getBalance()).isEqualByComparingTo(bd(125));
        assertThat(wallet.getInsuranceBet()).isEqualByComparingTo(bd(25));
    }
}
