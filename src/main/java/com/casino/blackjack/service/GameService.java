package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.BetHistoryEntity;
import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.PlayedGameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.repo.LastGameRepository;
import com.casino.blackjack.repo.PastGameRepository;
import com.casino.blackjack.repo.WalletRepository;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;
import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.casino.blackjack.util.LocalDateTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_00_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_01_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_04_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_05_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_06_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_07_EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_08_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_09_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_10_INSURANCE_YES_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_11_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_00_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_01_INVALID_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_02_LOW_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_03_HIGH_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.MAX_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.MIN_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_CURR_GAME_ERR;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_WALLET_FOUND;

@Service
public class GameService {

    private final LastGameRepository lastGameRepository;
    private final PastGameRepository pastGameRepository;
    private final WalletRepository walletRepository;

    private final UserService userService;
    private final BetHistoryService betHistoryService;

    private final LocalDateTimeProvider localDateTimeProvider;

    private final ObjectMapper om;

    public GameService(LastGameRepository lastGameRepository, PastGameRepository pastGameRepository,
                       WalletRepository walletRepository, UserService userService, BetHistoryService betHistoryService, LocalDateTimeProvider localDateTimeProvider, ObjectMapper om) {

        this.lastGameRepository = lastGameRepository;
        this.pastGameRepository = pastGameRepository;
        this.walletRepository = walletRepository;
        this.userService = userService;
        this.betHistoryService = betHistoryService;
        this.localDateTimeProvider = localDateTimeProvider;
        this.om = om;
    }

    // only visualize (return view dto from db entity)
    public Game getTable() {
        Optional<GameEntity> currentGameEntity = extractLastGame();
        Optional<WalletEntity> currentWalletEntity = extractWallet();
        WalletEntity currWalletEntity;

        if (currentWalletEntity.isEmpty()) {
            currWalletEntity = new WalletEntity().setOwner(userService.getCurrentLoggedUser());
            walletRepository.save(new WalletEntity().setOwner(userService.getCurrentLoggedUser()));
        } else {
            currWalletEntity = currentWalletEntity.get();
        }

        if (currentGameEntity.isPresent()) {

            GameEntity currGameEntity = currentGameEntity.get();
            Game game = Game.of(currGameEntity, om, currWalletEntity);

            if (game.getLastChoice().equals(CHOICE_09_INSURANCE_YES)) {

                BigDecimal currBalance = currWalletEntity.getBalance();
                BigDecimal currentBet = currWalletEntity.getCurrentBet();

                BigDecimal halfBet = currentBet.divide(new BigDecimal(2), new MathContext(3));

                if (halfBet.compareTo(currBalance) > 0) {
                    game.makeChoice(CHOICE_10_INSURANCE_YES_NOT_ENOUGH_MONEY)
                            .setInsurance(false)
                            .setAvailableChoices(List.of(CHOICE_11_INSURANCE_NO))
                            .setErrCodeList(List.of(ERR_CODE_00_INSUFFICIENT_FUNDS));
                } else {
                    Wallet wallet = Wallet.of(currWalletEntity)
                            .placeInsurance(halfBet);

                    game.setWallet(wallet);
                    WalletEntity.map(currWalletEntity, wallet);

                    walletRepository.save(currWalletEntity);
                }

                lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
            }

            if (game.getLastChoice().equals(CHOICE_10_INSURANCE_YES_NOT_ENOUGH_MONEY)) {
                BigDecimal currBalance = currWalletEntity.getBalance();
                BigDecimal currentBet = currWalletEntity.getCurrentBet();

                BigDecimal halfBet = currentBet.divide(new BigDecimal(2), new MathContext(3));

                if (halfBet.compareTo(currBalance) > 0) {
                    return game;
                } else {
                    Wallet wallet = Wallet.of(currWalletEntity);


                    game.setWallet(wallet)
                            .setErrCodeList(List.of())
                            .setAvailableChoices(List.of(CHOICE_09_INSURANCE_YES, CHOICE_11_INSURANCE_NO));

                    WalletEntity.map(currWalletEntity, wallet);

                    walletRepository.save(currWalletEntity);
                }

                lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
                return game;
            }

            if (!game.getErrCodeList().isEmpty()) {
                Game toReturn = game.setWallet(Wallet.of(currWalletEntity));
                Game gameClearErr = new Game(toReturn)
                        .setErrCodeList(Collections.emptyList());
                lastGameRepository.save(GameEntity.map(currGameEntity, gameClearErr, om));

                return toReturn;
            }

            if (currGameEntity.getFinalized()) {
                lastGameRepository.delete(currGameEntity);
                PlayedGameEntity playedGameEntity = PlayedGameEntity.of(currGameEntity)
                        .setFinalizedTime(localDateTimeProvider.getNow());

                pastGameRepository.save(playedGameEntity);

                BigDecimal totalBetAmount = currWalletEntity.payBet(currGameEntity.getHandMultiplier(),
                        currGameEntity.getInsuranceMultiplier());

                walletRepository.save(currWalletEntity);

                BetHistoryEntity betHistoryEntity = new BetHistoryEntity()
                        .setTotalBetAmount(totalBetAmount)
                        .setReturnAmount(currWalletEntity.getLastWin())
                        .setPlayedGame(playedGameEntity)
                        .setUser(playedGameEntity.getOwner());

                betHistoryService.save(betHistoryEntity);

                return Game.of(currGameEntity, om)
                        .setWallet(Wallet.of(currWalletEntity));
            }

            return Game.of(currGameEntity, om, currWalletEntity);
        }

        return new Game().setAvailableChoices(List.of(CHOICE_06_DEAL, CHOICE_00_CHIP_OPERATIONS))
                .setWallet(Wallet.of(currWalletEntity));
    }

    public void deal(String betStr) {

        Optional<GameEntity> currGameEntity = extractLastGame();
        Optional<WalletEntity> currWalletEntity = extractWallet();

        if (currWalletEntity.isEmpty()) {
            throw new IllegalStateException(NO_WALLET_FOUND);
        }

        WalletEntity walletEntity = currWalletEntity.get();
        Wallet wallet = Wallet.of(walletEntity);

        int validBet = validateBet(betStr, wallet);
        BigDecimal bet = new BigDecimal(betStr);

        if (validBet >= 0) { // invalid bet
            Game game = new Game().addErr(validBet)
                    .setAvailableChoices(List.of(CHOICE_06_DEAL, CHOICE_00_CHIP_OPERATIONS))
                    .setWallet(wallet);

            if (currGameEntity.isEmpty()) {
                lastGameRepository.save(GameEntity.of(game, om, userService.getCurrentLoggedUser()));
            } else {
                lastGameRepository.save(GameEntity.map(currGameEntity.get(), game, om));
            }

            return;
        }

        Game game = new Game().setDealt(true)
                .setHash(RNG.generateGameHash())
                .deal()
                .setDealtTime(localDateTimeProvider.getNow())
                .makeChoice(CHOICE_06_DEAL)
                .calcHand(false)
                .setWallet(wallet.placeHandBet(bet));

        GameEntity gameEntity;
        if (currGameEntity.isEmpty()) {
            gameEntity = GameEntity.of(game, om, userService.getCurrentLoggedUser());
        } else {
            gameEntity = GameEntity.map(currGameEntity.get(), game, om);
        }

        lastGameRepository.save(gameEntity);
        WalletEntity.map(walletEntity, game.getWallet());
        walletRepository.save(walletEntity);
    }

    public void even(Boolean evenChoice) {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game;
            if (evenChoice) {
                game = Game.of(currGameEntity, om)
                        .makeChoice(CHOICE_07_EVEN_MONEY_YES)
                        .calcHand(false);
            } else {
                game = Game.of(currGameEntity, om)
                        .makeChoice(CHOICE_08_EVEN_MONEY_NO)
                        .calcHand(false);
            }
            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    public void hit() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game = Game.of(currGameEntity, om)
                    .makeChoice(CHOICE_05_HIT)
                    .calcHand(!currGameEntity.getDealerSecondCardTen());

            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    public void stand() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game = Game.of(currGameEntity, om)
                    .makeChoice(CHOICE_04_STAND)
                    .calcHand(!currGameEntity.getDealerSecondCardTen());

            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    public void surrender() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game = Game.of(currGameEntity, om)
                    .makeChoice(CHOICE_01_SURRENDER)
                    .calcHand(false);

            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    public void insurance(Boolean insurance) {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game;
            if (insurance) {
                game = Game.of(currGameEntity, om)
                        .makeChoice(CHOICE_09_INSURANCE_YES)
                        .calcHand(!currGameEntity.getDealerSecondCardTen());
            } else {
                game = Game.of(currGameEntity, om)
                        .makeChoice(CHOICE_11_INSURANCE_NO)
                        .calcHand(!currGameEntity.getDealerSecondCardTen());
            }

            currGameEntity = GameEntity.map(currGameEntity, game, om);

            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    // GAME SERVICE HELPER METHODS (COMMONLY USED)
    private Optional<GameEntity> extractLastGame() {
        Long currentLoggedUserId = userService.getCurrentLoggedUserId();
        return lastGameRepository.findByOwnerId(currentLoggedUserId);
    }

    private Optional<WalletEntity> extractWallet() {
        Long currentLoggedUserId = userService.getCurrentLoggedUserId();
        return walletRepository.findByOwnerId(currentLoggedUserId);
    }

    private int validateBet(String betStr, Wallet wallet) {
        BigDecimal bet;

        try {
            bet = new BigDecimal(betStr);
        } catch (NumberFormatException e) {
            return ERR_CODE_01_INVALID_BET;
        }

        if (bet.compareTo(MIN_BET) < 0) {
            if (wallet.getBalance().compareTo(MIN_BET) < 0) {
                return ERR_CODE_00_INSUFFICIENT_FUNDS;
            }

            return ERR_CODE_02_LOW_BET;
        }

        if (bet.compareTo(MAX_BET) > 0) {
            return ERR_CODE_03_HIGH_BET;
        }

        if (bet.compareTo(wallet.getBalance()) > 0) {
            return ERR_CODE_00_INSUFFICIENT_FUNDS;
        }

        return -1;
    }
    // EO:GAME SERVICE HELPER METHODS (COMMONLY USED)
}
