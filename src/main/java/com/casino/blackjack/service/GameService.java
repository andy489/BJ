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

import java.util.List;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.util.Util.DEAL;
import static com.casino.blackjack.service.gamelogic.util.Util.EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.Util.EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.Util.HIT;
import static com.casino.blackjack.service.gamelogic.util.Util.INSURANCE_MULTIPLIER;
import static com.casino.blackjack.service.gamelogic.util.Util.INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.Util.INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.Util.NO_CURR_GAME_ERR;
import static com.casino.blackjack.service.gamelogic.util.Util.STAND;
import static com.casino.blackjack.service.gamelogic.util.Util.SURRENDER;

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
        Optional<GameEntity> gameEntity = extractLastGame();
        Optional<WalletEntity> walletEntity = extractWallet();

        if (gameEntity.isPresent() && walletEntity.isPresent()) {

            GameEntity currGameEntity = gameEntity.get();
            WalletEntity currWalletEntity = walletEntity.get();

            if (currGameEntity.getFinalized()) {
                lastGameRepository.delete(currGameEntity);
                pastGameRepository.save(PlayedGameEntity.of(currGameEntity));

                if (currGameEntity.getInsurance()) {
                    if (currGameEntity.getSecondDealerCardTen()) {
                        currWalletEntity.payBet(INSURANCE_MULTIPLIER);
                    } else {
                        currWalletEntity.payBet(currGameEntity.getWinMultiplier());
                    }
                }

                walletRepository.save(currWalletEntity);
            }

            return Game.of(currGameEntity, om, currWalletEntity);
        }
        System.out.println(new Game().setAvailableChoices(List.of(DEAL)).setWallet(new Wallet()));
        return new Game().setAvailableChoices(List.of(DEAL)).setWallet(new Wallet());
    }

    public void deal() {
        Optional<GameEntity> currGameEntity = extractLastGame();


        if (currGameEntity.isEmpty()) {
            Game game = new Game()
                    .setDealt(true)
                    .setHash(RNG.generateGameHash())
                    .deal()
                    .makeChoice(DEAL)
                    .calcHand();

            GameEntity gameEntity = GameEntity.of(game, om, userService.getCurrentLoggedUser());
            lastGameRepository.save(gameEntity);
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

//    public void even(Boolean evenChoice) {
//        Optional<GameEntity> gameEntity = extractLastGame();
//
//        if (gameEntity.isPresent()) {
//            GameEntity currGameEntity = gameEntity.get();
//
//            Game game;
//            if (evenChoice) {
//                game = Game.of(currGameEntity, om)
//                        .makeChoice(EVEN_MONEY_YES)
//                        .calcHand();
//            } else {
//                game = Game.of(currGameEntity, om)
//                        .makeChoice(EVEN_MONEY_NO)
//                        .calcHand();
//            }
//            currGameEntity = GameEntity.map(currGameEntity, game, om);
//            lastGameRepository.save(currGameEntity);
//            return;
//        }
//
//        throw new IllegalStateException(NO_CURR_GAME_ERR);
//    }
//
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
    // EO:GAME SERVICE HELPER METHODS (COMMONLY USED)
}
