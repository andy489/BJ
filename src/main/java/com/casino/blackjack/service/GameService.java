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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_NOT_BASIC_STRATEGY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INVALID_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_LOW_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_HIGH_BET;
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

    private final BasicStrategy basicStrategy;
    private final LocalDateTimeProvider localDateTimeProvider;

    private final ObjectMapper om;

    public GameService(LastGameRepository lastGameRepository, PastGameRepository pastGameRepository,
                       WalletRepository walletRepository, UserService userService, BetHistoryService betHistoryService, BasicStrategy basicStrategy, LocalDateTimeProvider localDateTimeProvider, ObjectMapper om) {

        this.lastGameRepository = lastGameRepository;
        this.pastGameRepository = pastGameRepository;
        this.walletRepository = walletRepository;
        this.userService = userService;
        this.betHistoryService = betHistoryService;
        this.basicStrategy = basicStrategy;
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

            BigDecimal currBalance = currWalletEntity.getBalance();
            BigDecimal currentBet = currWalletEntity.getCurrentBet();

            BigDecimal halfBet = BigDecimal.valueOf(currentBet.doubleValue())
                    .divide(BigDecimal.valueOf(2), new MathContext(3));

            // CONFIRM DOUBLE DOWN (WHEN NOT BASIC STRATEGY CHOICE)
            if(game.getLastChoice().equals(CHOICE_DOUBLE_DOWN_YES)){
                Wallet wallet = Wallet.of(currWalletEntity);
                wallet.doubleBet();
            }

            // INSURANCE OR DOUBLE DOWN
            if (game.getLastChoice().equals(CHOICE_INSURANCE_YES) || game.getLastChoice().equals(CHOICE_DOUBLE_DOWN)) {

                BigDecimal additionalBet;
                if (game.getLastChoice().equals(CHOICE_INSURANCE_YES)) { // INSURANCE
                    additionalBet = halfBet;
                } else { // DOUBLE DOWN
                    additionalBet = BigDecimal.valueOf(currentBet.doubleValue());
                }

                if (additionalBet.compareTo(currBalance) > 0) {
                    if (game.getLastChoice().equals(CHOICE_INSURANCE_YES)) { // INSURANCE
                        game.makeChoice(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)
                                .setInsurance(false)
                                .setAvailableChoices(List.of(CHOICE_INSURANCE_NO))
                                .setErrCodeList(List.of(ERR_CODE_INSUFFICIENT_FUNDS));
                    } else { // DOUBLE DOWN
                        game.makeChoice(CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY)
                                .setDoubleDown(false)
                                .setErrCodeList(List.of(ERR_CODE_INSUFFICIENT_FUNDS));
                    }
                } else {

                    Wallet wallet = Wallet.of(currWalletEntity);
                    if (game.getLastChoice().equals(CHOICE_INSURANCE_YES)) { // INSURANCE
                        game.setInsurance(true);
                        wallet.placeInsurance(additionalBet);
                    } else { // DOUBLE DOWN

                        Boolean shouldDoubleDown = basicStrategy.getDoubleDown(game);

                        if (shouldDoubleDown) {
                            game.setFinalized(true)
                                    .setDoubleDown(true)
                                    .calcHand();

                            wallet.doubleBet();
                        } else {
                            game.makeChoice(CHOICE_DOUBLE_NOT_BASIC_STRATEGY)
                                    .setAvailableChoices(List.of(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO))
                                    .setDoubleDown(false);
                            lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
                            return game;
                        }
                    }

                    game.setWallet(wallet);
                    WalletEntity.map(currWalletEntity, wallet);

                    walletRepository.save(currWalletEntity);
                }

                lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
            }

            if (game.getLastChoice().equals(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY) ||
                    game.getLastChoice().equals(CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY)) {

                BigDecimal additionalBet;
                if (game.getLastChoice().equals(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)) { // INSURANCE
                    additionalBet = halfBet;
                } else { // DOUBLE DOWN
                    additionalBet = BigDecimal.valueOf(currentBet.doubleValue());
                }

                if (additionalBet.compareTo(currBalance) > 0) { // ADDITIONAL_BET > CURR_BALANCE
                    lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
                    return game;
                } else { // ADDITIONAL_BET <= CURR_BALANCE
                    game.setAvailableChoices(List.of(CHOICE_INSURANCE_NO, CHOICE_INSURANCE_YES));
                }

                lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
                return game;
            }

            if (!game.getErrCodeList().isEmpty()) {
                Game toReturn = game.setWallet(Wallet.of(currWalletEntity));
                Game gameClearErr = new Game(toReturn)
                        .setErrCodeList(Collections.emptyList());
                lastGameRepository.save(GameEntity.map(currGameEntity, gameClearErr, om));

                return gameClearErr;
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

                if(game.getDoubleDown()){
                    betHistoryEntity.setDoubleDown(true);
                }

                betHistoryService.save(betHistoryEntity);

                return Game.of(currGameEntity, om)
                        .addAvailableChoice(CHOICE_CHIP_OPERATIONS)
                        .setWallet(Wallet.of(currWalletEntity));
            }

            return game;
        }

        return new Game()
                .setAvailableChoices(List.of(CHOICE_DEAL, CHOICE_CHIP_OPERATIONS))
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
                    .setAvailableChoices(List.of(CHOICE_DEAL, CHOICE_CHIP_OPERATIONS))
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
                .makeChoice(CHOICE_DEAL)
                .calcHand()
                .setWallet(wallet.placeHandBet(bet))
                .adjustDealerCardsAfterDeal();

        if (wallet.canDouble()) {
            game.removeAvailableChoice(CHOICE_DOUBLE_DOWN);
        }

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

    public void surrender() {
        choiceNoOption(CHOICE_SURRENDER, false);
    }

    public void stand() {
        choiceNoOption(CHOICE_STAND, true);
    }

    public void hit() {
        choiceNoOption(CHOICE_HIT, true);
    }

    public void insurance(Boolean insurance) {
        choiceOption(insurance, List.of(CHOICE_INSURANCE_YES, CHOICE_INSURANCE_NO));
    }

    public void doubleDown() {
        choiceNoOption(CHOICE_DOUBLE_DOWN, false);
    }

    public void ddConfirm(Boolean confirm) {
        choiceOption(confirm, List.of(CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO));
    }

    public void even(Boolean evenChoice) {
        choiceOption(evenChoice, List.of(CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO));
    }

    private void choiceNoOption(Integer makeChoice, Boolean calcHand) {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game = Game.of(currGameEntity, om)
                    .makeChoice(makeChoice);

            if (calcHand) {
                game.calcHand();
            }

            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    private void choiceOption(Boolean yesChoice, List<Integer> options) {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game;
            if (yesChoice) {
                game = Game.of(currGameEntity, om)
                        .makeChoice(options.get(0))
                        .calcHand();
            } else {
                game = Game.of(currGameEntity, om)
                        .makeChoice(options.get(1))
                        .calcHand();
            }

            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    public void accept() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();

            Game game = Game.of(currGameEntity, om);

//            if(game.getLastChoice().equals(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)){
//                game.removeAvailableChoice(CHOICE_DOUBLE_DOWN);
//            }

            game.clearErrors();

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
            return ERR_CODE_INVALID_BET;
        }

        if (bet.compareTo(MIN_BET) < 0) {
            if (wallet.getBalance().compareTo(MIN_BET) < 0) {
                return ERR_CODE_INSUFFICIENT_FUNDS;
            }

            return ERR_CODE_LOW_BET;
        }

        if (bet.compareTo(MAX_BET) > 0) {
            return ERR_CODE_HIGH_BET;
        }

        if (bet.compareTo(wallet.getBalance()) > 0) {
            return ERR_CODE_INSUFFICIENT_FUNDS;
        }

        return -1;
    }

    // EO:GAME SERVICE HELPER METHODS (COMMONLY USED)
}
