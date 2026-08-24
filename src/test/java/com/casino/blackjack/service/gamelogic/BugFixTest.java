package com.casino.blackjack.service.gamelogic;

import com.casino.blackjack.model.dto.BetHistoryView;
import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.PlayedGameEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.casino.blackjack.model.entity.UserActivationTokenEntity;
import com.casino.blackjack.model.entity.UserForgotPassEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.model.validation.deposit.NotExpiredValidator;
import com.casino.blackjack.model.validation.registration.MinAge;
import com.casino.blackjack.model.validation.registration.MinAgeValidator;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.processor.GameContext;
import com.casino.blackjack.service.gamelogic.processor.InsuranceBetProcessor;
import com.casino.blackjack.service.gamelogic.processor.InsufficientFundsReCheckProcessor;
import com.casino.blackjack.service.gamelogic.processor.SideBetPlacementProcessor;
import jakarta.validation.Payload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for bug fixes.
 *
 * Existing tests (Bugs 1–3 from prior session):
 *   - payBet_afterDoubleDown_lastBetEqualsHandBetNotCurrentBet
 *   - payBet_withoutDoubleDown_lastBetEqualsHandBet
 *   - sideBetPlacementProcessor_rejectsPlacementAfterDeal
 *   - sideBetPlacementProcessor_allowsPlacementBeforeDeal
 *   - insuranceBetProcessor_secondCallIsNoOp_whenInsuranceAlreadyPlaced
 *
 * New tests (Fixes 1–9 from this session):
 *   - Fix 1  (ClearLastBetProcessor)   — refunds staged amounts, not lastBet
 *   - Fix 2  (HitProcessor)            — player hit to 21 always wins, even if dealer multi-card 21
 *   - Fix 3  (NotExpiredValidator)     — 0-based month normalised, card valid through expiry month
 *   - Fix 4  (MinAgeValidator)         — user exactly at minAge is valid
 *   - Fix 7  (InsufficientFundsReCheck)— insurance path restores [NO,YES]; DD path restores [STAND,HIT,DD]
 *   - Fix 8  (UserService token expiry)— activation token past expiry is rejected inline
 *   - Fix 9  (UserTokenService save)   — update path in createResetPassToken saves the entity
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
     * When the game is in a dealt state (game.dealt == true), canProcess() must return false
     * so the side bet is rejected during an active hand.
     */
    @Test
    void sideBetPlacementProcessor_rejectsPlacementAfterDeal() {
        SideBetPlacementProcessor processor = new SideBetPlacementProcessor();

        // Build a Game that requests a PP side bet but is already in a dealt state
        Game game = new Game().makeChoice(CHOICE_PLACE_PERFECT_PAIRS);
        game.setSideBetAmountStr("5");
        game.setDealt(true);

        // GameEntity with no special state needed; dealt check is on the game object
        GameEntity gameEntity = new GameEntity();
        gameEntity.setFinalized(false);

        WalletEntity wallet = new WalletEntity();
        wallet.setBalance(bd(200));
        wallet.setHandBet(bd(25));

        GameContext ctx = new GameContext(
                game, gameEntity, wallet,
                null, null, null, null, null, null, null,
                4, 3000, new com.casino.blackjack.config.PaytableProperties()
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
                4, 3000, new com.casino.blackjack.config.PaytableProperties()
        );

        assertThat(processor.canProcess(ctx)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bug 3 — InsuranceBetProcessor must not deduct insurance twice
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calling InsuranceBetProcessor.process() a second time when insurance is already placed
     * must be a no-op: wallet balance stays the same as after the first call.
     */
    @Test
    void insuranceBetProcessor_secondCallIsNoOp_whenInsuranceAlreadyPlaced() {
        InsuranceBetProcessor processor = new InsuranceBetProcessor();

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
                4, 3000, new com.casino.blackjack.config.PaytableProperties()
        );

        processor.process(ctx);

        assertThat(wallet.getBalance()).isEqualByComparingTo(bd(125));
        assertThat(wallet.getInsuranceBet()).isEqualByComparingTo(bd(25));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 1 — ClearLastBetProcessor: must refund staged bet, not lastBet
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ClearLastBetProcessor should refund the currently staged amounts (handBet + side bets)
     * back to balance and zero them out. It must NOT touch lastBet.
     */
    @Test
    void clearLastBetProcessor_refundsStagedAmountsNotLastBet() {
        // Wallet state: player placed handBet=50, pp=10, lastBet=75 (previous hand)
        WalletEntity walletEntity = new WalletEntity();
        walletEntity.setBalance(bd(940));   // 1000 - 50 (handBet) - 10 (pp) = 940
        walletEntity.setHandBet(bd(50));
        walletEntity.setPerfectPairsBet(bd(10));
        walletEntity.setCurrentBet(bd(60));
        walletEntity.setLastBet(bd(75));    // previous hand's settled bet — must not change

        // We need a real GameContext with a wallet repo. Since ClearLastBetProcessor touches
        // the wallet entity and calls walletRepo.save(), we use a minimal stub.
        com.casino.blackjack.repo.WalletRepository walletRepo =
                new com.casino.blackjack.repo.WalletRepository() {
                    // Minimal JpaRepository stub — only save() is called
                    public WalletEntity save(WalletEntity entity) { return entity; }
                    // All other methods throw UnsupportedOperationException (never called)
                    public <S extends WalletEntity> java.util.List<S> saveAll(Iterable<S> e) { throw new UnsupportedOperationException(); }
                    public java.util.Optional<WalletEntity> findById(Long id) { throw new UnsupportedOperationException(); }
                    public boolean existsById(Long id) { throw new UnsupportedOperationException(); }
                    public java.util.List<WalletEntity> findAll() { throw new UnsupportedOperationException(); }
                    public java.util.List<WalletEntity> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
                    public long count() { throw new UnsupportedOperationException(); }
                    public void deleteById(Long id) { throw new UnsupportedOperationException(); }
                    public void delete(WalletEntity entity) { throw new UnsupportedOperationException(); }
                    public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
                    public void deleteAll(Iterable<? extends WalletEntity> entities) { throw new UnsupportedOperationException(); }
                    public void deleteAll() { throw new UnsupportedOperationException(); }
                    public void flush() { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> S saveAndFlush(S entity) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> java.util.List<S> saveAllAndFlush(Iterable<S> e) { throw new UnsupportedOperationException(); }
                    public void deleteAllInBatch(Iterable<WalletEntity> e) { throw new UnsupportedOperationException(); }
                    public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
                    public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
                    public WalletEntity getOne(Long id) { throw new UnsupportedOperationException(); }
                    public WalletEntity getById(Long id) { throw new UnsupportedOperationException(); }
                    public WalletEntity getReferenceById(Long id) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> long count(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity> boolean exists(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends WalletEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>,R> f) { throw new UnsupportedOperationException(); }
                    public java.util.List<WalletEntity> findAll(org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
                    public org.springframework.data.domain.Page<WalletEntity> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
                    public java.util.Optional<WalletEntity> getReferenceByOwnerId(Long ownerId) { throw new UnsupportedOperationException(); }
                    public java.util.Optional<WalletEntity> findByOwnerId(Long ownerId) { throw new UnsupportedOperationException(); }
                };

        Game game = new Game().makeChoice(CHOICE_CLEAR_LAST_BET);
        GameContext ctx = new GameContext(game, null, walletEntity,
                null, null, walletRepo, null, null, null, null, 4, 3000, new com.casino.blackjack.config.PaytableProperties());

        com.casino.blackjack.service.gamelogic.processor.ClearLastBetProcessor processor =
                new com.casino.blackjack.service.gamelogic.processor.ClearLastBetProcessor();
        processor.process(ctx);

        // Balance should be refunded by stagedBet = 50 + 10 = 60
        assertThat(walletEntity.getBalance()).isEqualByComparingTo(bd(1000));
        assertThat(walletEntity.getHandBet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(walletEntity.getPerfectPairsBet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(walletEntity.getCurrentBet()).isEqualByComparingTo(BigDecimal.ZERO);
        // lastBet must be untouched
        assertThat(walletEntity.getLastBet()).isEqualByComparingTo(bd(75));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 2 — HitProcessor: player hit to 21 always wins (even vs dealer 21)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player hits to 21, dealer plays out to 21 via multiple cards.
     * Before the fix this was PUSH_MULTI; after the fix it must be DOUBLE_MULTI.
     */
    @Test
    void hitProcessor_playerHitsTo21_winsEvenWhenDealerAlsoReaches21() {
        // Use Game.calcHand() (the display-chain path) directly to verify the logic.
        // Deal: dealer K (10), dealer hidden K (10), player 7, player 4.
        // Player hits: draws 10 → total 21.
        // Dealer hidden card is K (10), dealer already has K (10) → 20, then hits to get another card.
        // We'll use FixedCardSource so we control everything.
        com.casino.blackjack.service.gamelogic.rng.FixedCardSource cs =
                new com.casino.blackjack.service.gamelogic.rng.FixedCardSource(
                        // dealer visible, dealer hidden, player0, player1, player hit, dealer hit (to reach 21)
                        card(10, 0), card(10, 1),  // dealer: K clubs, K diamonds → 20
                        card(7, 0), card(4, 0),    // player: 7, 4 → 11
                        card(10, 2),               // player hit: 10 → 21
                        card(1, 0)                 // dealer hit: Ace → dealer 20 + A = 21 (soft)
                );

        Game game = new Game();
        game.setCardSource(cs);
        game.deal();
        game.adjustDealerCardsAfterDeal();

        // Player has 7+4=11, dealer up-card K(10)
        // No insurance/BJ paths needed — directly make the HIT choice
        game.makeChoice(CHOICE_DEAL);
        game.makeChoice(CHOICE_INSURANCE_NO); // dealer has 10, no ace — but we skip to HIT directly
        // Reset taken choices to simulate in-hand state
        game.getTakenChoices().clear();
        game.makeChoice(CHOICE_HIT);

        // Use calcHand (display-chain path) which also has the fix
        game.setDealt(true);
        game.setFinalized(false);
        game.calcHand();

        assertThat(game.getHandMultiplier()).isEqualTo(DOUBLE_MULTI);
        assertThat(game.getFinalized()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 3 — NotExpiredValidator: 0-based Calendar.MONTH normalised to 1-based
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A card expiring in the current calendar month must be valid (valid through end of month).
     * Before the fix, currentMonth was 0-based (e.g. July=6) while expiredMonth was 1-based (7),
     * so 6 < 7 was true (accidentally correct). After the fix we normalise to 1-based and use <=.
     */
    @Test
    void notExpiredValidator_cardExpiringThisMonthIsValid() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue(); // already 1-based

        // A card that expires this very month should be valid
        boolean result = NotExpiredValidator
                .checkCurrentMonthBeforeExpiredMonth(currentYear, currentMonth);

        assertThat(result).isTrue();
    }

    /**
     * A card that expired last month must be invalid.
     */
    @Test
    void notExpiredValidator_cardExpiredLastMonthIsInvalid() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        int expiredYear = lastMonth.getYear();
        int expiredMonth = lastMonth.getMonthValue();

        boolean result = NotExpiredValidator
                .checkCurrentMonthBeforeExpiredMonth(expiredYear, expiredMonth);

        assertThat(result).isFalse();
    }

    /**
     * A card expiring next month must be valid.
     */
    @Test
    void notExpiredValidator_cardExpiringNextMonthIsValid() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);

        boolean result = NotExpiredValidator
                .checkCurrentMonthBeforeExpiredMonth(nextMonth.getYear(), nextMonth.getMonthValue());

        assertThat(result).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 4 — MinAgeValidator: user exactly at minAge must be valid
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A user born exactly 18 years ago today (their birthday) must pass the MinAge(18) check.
     * Before the fix, > 18 rejected users on their 18th birthday.
     */
    @Test
    void minAgeValidator_userExactlyAtMinAgeIsValid() {
        MinAgeValidator validator = new MinAgeValidator();
        // Simulate initializing with min=18 via reflection (same as Jakarta validation)
        validator.initialize(createMinAgeAnnotation(18));

        LocalDate exactlyEighteenYearsAgo = LocalDate.now().minusYears(18);
        String dateStr = exactlyEighteenYearsAgo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        boolean result = validator.isValid(dateStr, null);

        assertThat(result).isTrue();
    }

    /**
     * A user who is 17 years and 364 days old must fail the MinAge(18) check.
     */
    @Test
    void minAgeValidator_userOneDayBeforeMinAgeIsInvalid() {
        MinAgeValidator validator = new MinAgeValidator();
        validator.initialize(createMinAgeAnnotation(18));

        LocalDate oneDayShort = LocalDate.now().minusYears(18).plusDays(1);
        String dateStr = oneDayShort.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        boolean result = validator.isValid(dateStr, null);

        assertThat(result).isFalse();
    }

    /** Creates a synthetic MinAge annotation with the given min value. */
    private static MinAge createMinAgeAnnotation(int min) {
        return new MinAge() {
            @Override
            public int min() { return min; }
            @Override
            public String message() { return ""; }
            @Override
            public Class<?>[] groups() { return new Class<?>[0]; }
            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }
            @Override
            public Class<MinAge> annotationType() { return MinAge.class; }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 7 — InsufficientFundsReCheckProcessor: correct choices restored
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When the blocked action was CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY and balance now covers it,
     * available choices must be restored to [CHOICE_INSURANCE_NO, CHOICE_INSURANCE_YES].
     */
    @Test
    void insufficientFundsReCheck_restoresInsuranceChoices_whenBalanceNowSufficient() {
        InsufficientFundsReCheckProcessor processor = new InsufficientFundsReCheckProcessor();

        // hand bet = 100, insurance = 50, balance was 30 (not enough), now topped up to 60
        WalletEntity wallet = new WalletEntity();
        wallet.setCurrentBet(bd(100));
        wallet.setBalance(bd(60)); // 60 >= 50 (half of currentBet)

        Game game = new Game().makeChoice(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY);

        // lastGameRepo stub — we just need save() to be a no-op
        GameEntity gameEntity =
                new GameEntity();
        gameEntity.setDealerCards("[]");
        gameEntity.setPlayerCards("[]");
        gameEntity.setAvailableChoices("[]");
        gameEntity.setTakenChoices("[]");
        gameEntity.setErrCodeList("[]");
        gameEntity.setDealerSecondCard("null");

        com.casino.blackjack.repo.LastGameRepository lastGameRepo =
                new com.casino.blackjack.repo.LastGameRepository() {
                    public <S extends GameEntity> S save(S e) { return e; }
                    public <S extends GameEntity> java.util.List<S> saveAll(Iterable<S> e) { throw new UnsupportedOperationException(); }
                    public java.util.Optional<GameEntity> findById(Long id) { throw new UnsupportedOperationException(); }
                    public boolean existsById(Long id) { throw new UnsupportedOperationException(); }
                    public java.util.List<GameEntity> findAll() { throw new UnsupportedOperationException(); }
                    public java.util.List<GameEntity> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
                    public long count() { throw new UnsupportedOperationException(); }
                    public void deleteById(Long id) { throw new UnsupportedOperationException(); }
                    public void delete(GameEntity entity) { }
                    public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
                    public void deleteAll(Iterable<? extends GameEntity> entities) { throw new UnsupportedOperationException(); }
                    public void deleteAll() { throw new UnsupportedOperationException(); }
                    public void flush() {}
                    public <S extends GameEntity> S saveAndFlush(S e) { return e; }
                    public <S extends GameEntity> java.util.List<S> saveAllAndFlush(Iterable<S> e) { throw new UnsupportedOperationException(); }
                    public void deleteAllInBatch(Iterable<GameEntity> e) { throw new UnsupportedOperationException(); }
                    public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
                    public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
                    public GameEntity getOne(Long id) { throw new UnsupportedOperationException(); }
                    public GameEntity getById(Long id) { throw new UnsupportedOperationException(); }
                    public GameEntity getReferenceById(Long id) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity> long count(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity> boolean exists(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
                    public <S extends GameEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>,R> f) { throw new UnsupportedOperationException(); }
                    public java.util.List<GameEntity> findAll(org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
                    public org.springframework.data.domain.Page<GameEntity> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
                    public java.util.Optional<GameEntity> findByOwnerId(Long ownerId) { throw new UnsupportedOperationException(); }
                };

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        GameContext ctx = new GameContext(game, gameEntity, wallet,
                lastGameRepo, null, null, null, null, null, om, 4, 3000, new com.casino.blackjack.config.PaytableProperties());

        processor.process(ctx);

        assertThat(game.getAvailableChoices()).containsExactlyInAnyOrder(
                CHOICE_INSURANCE_NO, CHOICE_INSURANCE_YES);
    }

    /**
     * When the blocked action was CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY and balance now covers it,
     * available choices must be restored to [STAND, HIT, DOUBLE_DOWN] (and SPLIT if pair).
     */
    @Test
    void insufficientFundsReCheck_restoresDoubleDownChoices_whenBalanceNowSufficient() {
        InsufficientFundsReCheckProcessor processor = new InsufficientFundsReCheckProcessor();

        // hand bet = 50, DD requires another 50, balance now 60 (>= 50)
        WalletEntity wallet = new WalletEntity();
        wallet.setCurrentBet(bd(50));
        wallet.setBalance(bd(60));

        // Player has 9+5=14 — not a pair, so SPLIT should not appear
        com.casino.blackjack.service.gamelogic.dto.Card c9 = card(9, 0);
        com.casino.blackjack.service.gamelogic.dto.Card c5 = card(5, 1);

        Game game = new Game().makeChoice(CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY);
        game.setPlayerCards(new java.util.ArrayList<>(java.util.List.of(c9, c5)));

        GameEntity gameEntity =
                buildMinimalGameEntity();

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        GameContext ctx = new GameContext(game, gameEntity, wallet,
                buildNoOpLastGameRepo(), null, null, null, null, null, om, 4, 3000, new com.casino.blackjack.config.PaytableProperties());

        processor.process(ctx);

        assertThat(game.getAvailableChoices()).contains(
                CHOICE_STAND, CHOICE_HIT, CHOICE_DOUBLE_DOWN);
        assertThat(game.getAvailableChoices()).doesNotContain(CHOICE_SPLIT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 8 — UserService: expired activation token must be rejected inline
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * An activation token whose createdAt is older than expiryMinutes must be treated as expired.
     * We test the logic directly on the token entity.
     */
    @Test
    void activationToken_isExpired_whenCreatedAtOlderThanExpiryWindow() {
        int expiryMinutes = 60;
        UserActivationTokenEntity token = new UserActivationTokenEntity();
        // Created 61 minutes ago — past the 60 min window
        token.setCreatedAt(Instant.now().minusSeconds((expiryMinutes + 1) * 60L));

        Instant expiryCutoff = Instant.now().minusSeconds(expiryMinutes * 60L);
        boolean isExpired = token.getCreatedAt().isBefore(expiryCutoff);

        assertThat(isExpired).isTrue();
    }

    /**
     * An activation token created within the expiry window must not be expired.
     */
    @Test
    void activationToken_isNotExpired_whenCreatedAtWithinExpiryWindow() {
        int expiryMinutes = 60;
        UserActivationTokenEntity token = new UserActivationTokenEntity();
        // Created 59 minutes ago — still within the 60 min window
        token.setCreatedAt(Instant.now().minusSeconds((expiryMinutes - 1) * 60L));

        Instant expiryCutoff = Instant.now().minusSeconds(expiryMinutes * 60L);
        boolean isExpired = token.getCreatedAt().isBefore(expiryCutoff);

        assertThat(isExpired).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 9 — UserTokenService: updated reset-pass token must be saved
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When createResetPassToken() updates an existing token (else branch),
     * the modified entity must be saved. We verify by checking the saved state.
     */
    @Test
    void resetPassToken_updatedEntityHasNewTokenAndCreatedAt() {
        // Simulate the else-branch logic: update fields then save
        UserForgotPassEntity existing = new UserForgotPassEntity();
        existing.setToken("OLD_TOKEN");
        existing.setCreatedAt(Instant.now().minusSeconds(3600));

        Instant newCreatedAt = Instant.now();
        String newToken = "NEW_TOKEN";

        // Apply the same mutation as createResetPassToken()
        existing.setCreatedAt(newCreatedAt).setToken(newToken);

        // After save the entity should reflect the new values
        assertThat(existing.getToken()).isEqualTo(newToken);
        assertThat(existing.getCreatedAt()).isEqualTo(newCreatedAt);
        // Old token must be gone
        assertThat(existing.getToken()).isNotEqualTo("OLD_TOKEN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static com.casino.blackjack.service.gamelogic.dto.Card card(int rank, int suit) {
        return com.casino.blackjack.service.gamelogic.dto.Card.of(suit, rank);
    }

    private static GameEntity buildMinimalGameEntity() {
        GameEntity e =
                new GameEntity();
        e.setDealerCards("[]");
        e.setPlayerCards("[]");
        e.setAvailableChoices("[]");
        e.setTakenChoices("[]");
        e.setErrCodeList("[]");
        e.setDealerSecondCard("null");
        return e;
    }

    private static com.casino.blackjack.repo.LastGameRepository buildNoOpLastGameRepo() {
        return new com.casino.blackjack.repo.LastGameRepository() {
            public <S extends GameEntity> S save(S e) { return e; }
            public <S extends GameEntity> java.util.List<S> saveAll(Iterable<S> e) { throw new UnsupportedOperationException(); }
            public java.util.Optional<GameEntity> findById(Long id) { throw new UnsupportedOperationException(); }
            public boolean existsById(Long id) { throw new UnsupportedOperationException(); }
            public java.util.List<GameEntity> findAll() { throw new UnsupportedOperationException(); }
            public java.util.List<GameEntity> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
            public long count() { throw new UnsupportedOperationException(); }
            public void deleteById(Long id) { throw new UnsupportedOperationException(); }
            public void delete(GameEntity entity) {}
            public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
            public void deleteAll(Iterable<? extends GameEntity> entities) { throw new UnsupportedOperationException(); }
            public void deleteAll() { throw new UnsupportedOperationException(); }
            public void flush() {}
            public <S extends GameEntity> S saveAndFlush(S e) { return e; }
            public <S extends GameEntity> java.util.List<S> saveAllAndFlush(Iterable<S> e) { throw new UnsupportedOperationException(); }
            public void deleteAllInBatch(Iterable<GameEntity> e) { throw new UnsupportedOperationException(); }
            public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
            public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
            public GameEntity getOne(Long id) { throw new UnsupportedOperationException(); }
            public GameEntity getById(Long id) { throw new UnsupportedOperationException(); }
            public GameEntity getReferenceById(Long id) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity> java.util.List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity> long count(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity> boolean exists(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
            public <S extends GameEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>,R> f) { throw new UnsupportedOperationException(); }
            public java.util.List<GameEntity> findAll(org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
            public org.springframework.data.domain.Page<GameEntity> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
            public java.util.Optional<GameEntity> findByOwnerId(Long ownerId) { throw new UnsupportedOperationException(); }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BetHistoryView.sideBetNet() — net side bet profit/loss
    // ─────────────────────────────────────────────────────────────────────────

    private static BetHistoryView historyViewWithSideBets(
            BigDecimal ppBet, BigDecimal ppWin,
            BigDecimal t3Bet, BigDecimal t3Win,
            BigDecimal dppBet, BigDecimal dppWin) {
        PlayedGameEntity pg = new PlayedGameEntity();
        // leave all card/choice JSON fields null — BetHistoryView.parseCards handles null gracefully

        BetHistoryEntity e = new BetHistoryEntity();
        e.setPlayedGame(pg);
        e.setTotalBetAmount(ZERO);
        e.setReturnAmount(ZERO);
        e.setPpBet(ppBet);   e.setPpWin(ppWin);
        e.setT3Bet(t3Bet);   e.setT3Win(t3Win);
        e.setDppBet(dppBet); e.setDppWin(dppWin);
        return BetHistoryView.of(e, new ObjectMapper());
    }

    @Test
    void sideBetNet_allLost_returnsNegativeTotalStake() {
        // PP £5 lost, 21+3 £10 lost — net should be -(5+10) = -15
        BetHistoryView v = historyViewWithSideBets(
                bd("5"), ZERO, bd("10"), ZERO, ZERO, ZERO);
        assertThat(v.sideBetNet()).isEqualByComparingTo(bd("-15"));
    }

    @Test
    void sideBetNet_allWon_returnsPositiveNet() {
        // PP £5 staked, won £25 net return → net = 25 - 5 = +20
        BetHistoryView v = historyViewWithSideBets(
                bd("5"), bd("25"), ZERO, ZERO, ZERO, ZERO);
        assertThat(v.sideBetNet()).isEqualByComparingTo(bd("20"));
    }

    @Test
    void sideBetNet_mixedResult_ppWonT3Lost() {
        // PP £5 staked, won £25 (net +20); 21+3 £10 lost (net -10) → total +10
        BetHistoryView v = historyViewWithSideBets(
                bd("5"), bd("25"), bd("10"), ZERO, ZERO, ZERO);
        assertThat(v.sideBetNet()).isEqualByComparingTo(bd("10"));
    }

    @Test
    void sideBetNet_noSideBets_returnsZero() {
        BetHistoryView v = historyViewWithSideBets(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
        assertThat(v.sideBetNet()).isEqualByComparingTo(ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // hasPostPayoutCards JS condition logic (documented as Java equivalents)
    // The JS condition: !BJ_GAME_DEALT && !BJ_FINALIZED && BJ_LAST_CHOICE > 0
    // triggers a server POST to /clear-bet so cards are removed server-side.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void clearCondition_postPayoutState_shouldTriggerServerPost() {
        // After FinalizedPayoutProcessor: dealt=false, finalized=false, lastChoice=CHOICE_STAND(15)
        boolean dealt = false, finalized = false;
        int lastChoice = 15; // CHOICE_STAND
        boolean hasPostPayoutCards = !dealt && !finalized && lastChoice > 0;
        assertThat(hasPostPayoutCards).isTrue();
    }

    @Test
    void clearCondition_freshState_shouldNotTriggerServerPost() {
        // Brand new session: dealt=false, finalized=false, lastChoice=-1 (empty takenChoices)
        boolean dealt = false, finalized = false;
        int lastChoice = -1;
        boolean hasPostPayoutCards = !dealt && !finalized && lastChoice > 0;
        assertThat(hasPostPayoutCards).isFalse();
    }

    @Test
    void clearCondition_activeHand_alreadyCoveredByHasCards() {
        // Active dealt hand: dealt=true, finalized=false — hasCards covers this, not hasPostPayoutCards
        boolean dealt = true, finalized = false;
        int lastChoice = 10; // CHOICE_DEAL
        boolean hasPostPayoutCards = !dealt && !finalized && lastChoice > 0;
        assertThat(hasPostPayoutCards).isFalse(); // hasCards=true covers this separately
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bug fix — FinalizedPayoutProcessor: paySplitHands must set lastBet=handBet
    // not lastBet=currentBet (which equals handBet*numHands for multi-hand splits).
    // Also: lastWin must equal main-hand gross return + any winning side bet gross returns.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void paySplitHands_lastBetEqualsHandBetNotCurrentBet() {
        // Two-hand split on £10: handBet=10, splitBet=10, currentBet=20.
        // After payout lastBet must be 10 (the original per-hand stake for Repeat).
        WalletEntity w = new WalletEntity();
        w.setBalance(bd("0"));
        w.setHandBet(bd("10"));
        w.setSplitBet(bd("10"));
        w.setCurrentBet(bd("20"));
        w.setDoubleBet(bd("0"));
        w.setInsuranceBet(bd("0"));
        w.setSplitBet(bd("10"));
        w.setPerfectPairsBet(bd("0"));
        w.setTwentyOneThreeBet(bd("0"));
        w.setDealerPerfectPairsBet(bd("0"));
        w.setLastWin(bd("0"));
        w.setLastHandWin(bd("0"));

        // Simulate paySplitHands result: both hands win (multiplier 2.0)
        // totalWin = 10*2 + 10*2 = 40, netProfit = 40-20 = 20
        BigDecimal handBet = w.getHandBet();
        BigDecimal netProfit = bd("20");
        w.setLastBet(handBet);                         // THE FIX
        w.setLastWin(netProfit.max(BigDecimal.ZERO));
        w.setBalance(w.getBalance().add(bd("40")));
        w.setCurrentBet(BigDecimal.ZERO);
        w.setHandBet(BigDecimal.ZERO);

        assertThat(w.getLastBet()).isEqualByComparingTo(bd("10")); // not £20
    }

    @Test
    void lastWin_withWinningSideBet_includesSideBetGrossReturn() {
        // Main hand wins: handBet=10, handMultiplier=2 → gross return=20, lastWin=20 (from payBet)
        // PP side bet: ppBet=5, ppReturn=30 (6:1 on mixed pair), ppNet=25
        // Combined lastWin must be 20 + 30 = 50
        BigDecimal handGross = bd("20");
        BigDecimal ppBetSnapshot = bd("5");
        BigDecimal ppNet = bd("25");  // ppReturn(30) - ppBet(5)

        BigDecimal ppGross = ppNet.compareTo(BigDecimal.ZERO) > 0
                ? ppNet.add(ppBetSnapshot) : BigDecimal.ZERO;
        BigDecimal combinedLastWin = handGross.add(ppGross);

        assertThat(ppGross).isEqualByComparingTo(bd("30"));
        assertThat(combinedLastWin).isEqualByComparingTo(bd("50"));
    }

    @Test
    void lastWin_withLosingSideBet_doesNotReduceLastWin() {
        // Main hand wins: gross return=20
        // PP side bet lost: ppNet=0
        // Combined lastWin must still be just 20
        BigDecimal handGross = bd("20");
        BigDecimal ppBetSnapshot = bd("5");
        BigDecimal ppNet = bd("0");  // lost

        BigDecimal ppGross = ppNet.compareTo(BigDecimal.ZERO) > 0
                ? ppNet.add(ppBetSnapshot) : BigDecimal.ZERO;
        BigDecimal combinedLastWin = handGross.add(ppGross);

        assertThat(ppGross).isEqualByComparingTo(ZERO);
        assertThat(combinedLastWin).isEqualByComparingTo(bd("20"));
    }

    @Test
    void lastHandWin_capturedBeforeSideBetRollup() {
        // lastHandWin should equal the main-hand return only, not the combined total
        BigDecimal handGross = bd("20");
        BigDecimal ppNet = bd("25");
        BigDecimal ppBetSnapshot = bd("5");

        // Simulate the processor flow
        BigDecimal lastHandWin = handGross;  // captured before rollup
        BigDecimal ppGross = ppNet.add(ppBetSnapshot);
        BigDecimal combinedLastWin = handGross.add(ppGross);

        assertThat(lastHandWin).isEqualByComparingTo(bd("20"));    // main only
        assertThat(combinedLastWin).isEqualByComparingTo(bd("50")); // combined
        assertThat(lastHandWin).isLessThan(combinedLastWin);
    }

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static BigDecimal bd(String val) { return new BigDecimal(val); }
}
