package com.casino.blackjack.service;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.util.Util.CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.Util.DEAL;
import static com.casino.blackjack.service.gamelogic.util.Util.ERR_CODE_HIGH_BET;
import static com.casino.blackjack.service.gamelogic.util.Util.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.Util.ERR_CODE_INVALID_BET;
import static com.casino.blackjack.service.gamelogic.util.Util.ERR_CODE_LOW_BET;
import static com.casino.blackjack.service.gamelogic.util.Util.EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.Util.EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.Util.MAX_BET;
import static com.casino.blackjack.service.gamelogic.util.Util.MIN_BET;
import static com.casino.blackjack.service.gamelogic.util.Util.NO_CURR_GAME_ERR;
import static com.casino.blackjack.service.gamelogic.util.Util.NO_WALLET_FOUND;

@Service
public class GameService {

    private final LastGameRepository lastGameRepository;
    private final PastGameRepository pastGameRepository;
    private final WalletRepository walletRepository;

    private final UserService userService;

    private final ObjectMapper om;

    public GameService(LastGameRepository lastGameRepository, PastGameRepository pastGameRepository,
                       WalletRepository walletRepository, UserService userService, ObjectMapper om) {

        this.lastGameRepository = lastGameRepository;
        this.pastGameRepository = pastGameRepository;
        this.walletRepository = walletRepository;
        this.userService = userService;
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

            if (!game.getErrCodeList().isEmpty()) {
                Game toReturn = game.setWallet(Wallet.of(currWalletEntity));
                Game gameClearErr = new Game(toReturn)
                        .setErrCodeList(Collections.emptyList());
                lastGameRepository.save(GameEntity.map(currGameEntity, gameClearErr, om));

                return toReturn;
            }

            if (currGameEntity.getFinalized()) {
                lastGameRepository.delete(currGameEntity);
                PlayedGameEntity playedGameEntity = PlayedGameEntity.of(currGameEntity);
                pastGameRepository.save(playedGameEntity);

                currWalletEntity.payBet(playedGameEntity.getWinMultiplier());

//                if (currGameEntity.getInsurance()) {
//                    if (currGameEntity.getSecondDealerCardTen()) {
//                        currWalletEntity.payBet(INSURANCE_MULTIPLIER);
//                    } else {
//                        currWalletEntity.payBet(currGameEntity.getWinMultiplier());
//                    }
//                }

                walletRepository.save(currWalletEntity);

                return Game.of(currGameEntity, om)
                        .setWallet(Wallet.of(currWalletEntity));
            }

            return Game.of(currGameEntity, om, currWalletEntity);
        }

        return new Game().setAvailableChoices(List.of(DEAL, CHIP_OPERATIONS))
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

        if (validBet > 0) { // invalid bet
            Game game = new Game().addErr(validBet)
                    .setAvailableChoices(List.of(DEAL, CHIP_OPERATIONS))
                    .setWallet(wallet);

            if (currGameEntity.isEmpty()) {
                lastGameRepository.save(GameEntity.of(game, om, userService.getCurrentLoggedUser()));
            } else {
                lastGameRepository.save(GameEntity.map(currGameEntity.get(), game, om));
            }

            return;
        }

        Game game = new Game()
                .setDealt(true)
                .setHash(RNG.generateGameHash())
                .deal()
                .makeChoice(DEAL)
                .calcHand()
                .setWallet(wallet.placeBet(bet));

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
                        .makeChoice(EVEN_MONEY_YES)
                        .calcHand();
            } else {
                game = Game.of(currGameEntity, om)
                        .makeChoice(EVEN_MONEY_NO)
                        .calcHand();
            }
            currGameEntity = GameEntity.map(currGameEntity, game, om);
            lastGameRepository.save(currGameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

//    public void hit() {
//        Optional<GameEntity> gameEntity = extractLastGame();
//
//        if (gameEntity.isPresent()) {
//            GameEntity currGameEntity = gameEntity.get();
//
//            Game game = Game.of(currGameEntity, om)
//                    .makeChoice(HIT)
//                    .calcHand();
//
//            currGameEntity = GameEntity.map(currGameEntity, game, om);
//            lastGameRepository.save(currGameEntity);
//            return;
//        }
//
//        throw new IllegalStateException(NO_CURR_GAME_ERR);
//    }
//
//    public void stand() {
//        Optional<GameEntity> gameEntity = extractLastGame();
//
//        if (gameEntity.isPresent()) {
//            GameEntity currGameEntity = gameEntity.get();
//
//            Game game = Game.of(currGameEntity, om)
//                    .makeChoice(STAND)
//                    .calcHand();
//
//            currGameEntity = GameEntity.map(currGameEntity, game, om);
//            lastGameRepository.save(currGameEntity);
//            return;
//        }
//
//        throw new IllegalStateException(NO_CURR_GAME_ERR);
//    }
//
//    public void surrender() {
//        Optional<GameEntity> gameEntity = extractLastGame();
//
//        if (gameEntity.isPresent()) {
//            GameEntity currGameEntity = gameEntity.get();
//
//            Game game = Game.of(currGameEntity, om)
//                    .makeChoice(SURRENDER)
//                    .calcHand();
//
//            currGameEntity = GameEntity.map(currGameEntity, game, om);
//            lastGameRepository.save(currGameEntity);
//            return;
//        }
//
//        throw new IllegalStateException(NO_CURR_GAME_ERR);
//    }

//    public void insurance(Boolean makeInsurance) {
//        Optional<GameEntity> gameEntity = extractLastGame();
//
//        if (gameEntity.isPresent()) {
//            GameEntity currGameEntity = gameEntity.get();
//
//            Game game;
//            if (makeInsurance) {
//                game = Game.of(currGameEntity, om)
//                        .makeChoice(INSURANCE_YES)
//                        .calcHand();
//            } else {
//                game = Game.of(currGameEntity, om)
//                        .makeChoice(INSURANCE_NO)
//                        .calcHand();
//            }
//            currGameEntity = GameEntity.map(currGameEntity, game, om);
//            lastGameRepository.save(currGameEntity);
//            return;
//        }
//
//        throw new IllegalStateException(NO_CURR_GAME_ERR);
//    }

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
            return ERR_CODE_LOW_BET;
        }

        if (bet.compareTo(MAX_BET) > 0) {
            return ERR_CODE_HIGH_BET;
        }

        if (bet.compareTo(wallet.getBalance()) > 0) {
            return ERR_CODE_INSUFFICIENT_FUNDS;
        }

        return 0;
    }
    // EO:GAME SERVICE HELPER METHODS (COMMONLY USED)
}
