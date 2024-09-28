package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.PastGameEntity;
import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.repo.LastGameRepository;
import com.casino.blackjack.repo.PastGameRepository;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameService {

    private final LastGameRepository lastGameRepository;
    private final PastGameRepository pastGameRepository;

    private final UserService userService;

    private final ObjectMapper om;

    public GameService(LastGameRepository lastGameRepository, PastGameRepository pastGameRepository,
                       UserService userService, ObjectMapper om) {
        this.lastGameRepository = lastGameRepository;
        this.pastGameRepository = pastGameRepository;
        this.userService = userService;
        this.om = om;
    }

    public Game sitOnTable() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            Game gameView;
            GameEntity currGame = gameEntity.get();
            try {
                gameView = new Game()
                        .setHash(currGame.getHash())
                        .setDealt(true)
                        .setDealerCards(om.readValue(currGame.getDealerCards(), new TypeReference<>() {
                        }))
                        .setPlayerCards(om.readValue(currGame.getPlayerCards(), new TypeReference<>() {
                        }))
                        .setAvailableDecisions(om.readValue(currGame.getAvailableDecisions(), new TypeReference<>() {
                        }));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (currGame.getFinalized()) {
                lastGameRepository.delete(currGame);
                pastGameRepository.save(PastGameEntity.of(currGame));
            }

            return gameView;
        }

        return new Game().calcHand();
    }

    public void deal() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGame = gameEntity.get();
            if (currGame.getFinalized()) {
                lastGameRepository.delete(currGame);
                pastGameRepository.save(PastGameEntity.of(currGame));
            }
        }

        Game game = new Game()
                .setDealt(true)
                .setHash(RNG.generateGameHash())
                .deal()
                .calcHand();

        save(game);
    }

    private Optional<GameEntity> extractLastGame() {
        Long currentLoggedUserId = userService.getCurrentLoggedUserId();
        return lastGameRepository.findByOwnerId(currentLoggedUserId);
    }

    private void save(Game game) {

        UserEntity currentLoggedUser = userService.getCurrentLoggedUser();

        try {
            GameEntity gameEntity = new GameEntity()
                    .setHash(game.getHash())
                    .setDealerCards(om.writeValueAsString(game.getDealerCards()))
                    .setPlayerCards(om.writeValueAsString(game.getPlayerCards()))
                    .setAvailableDecisions(om.writeValueAsString(game.getAvailableDecisions()))
                    .setFinalized(game.getFinalized())
                    .setWinMultiplier(game.getWinMultiplier())
                    .setOwner(currentLoggedUser);

            lastGameRepository.save(gameEntity);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
