package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.repo.LastGameRepository;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameService {

    private final LastGameRepository lastGameRepository;

    private final UserService userService;

    private final ObjectMapper om;

    public GameService(LastGameRepository lastGameRepository, UserService userService, ObjectMapper om) {
        this.lastGameRepository = lastGameRepository;
        this.userService = userService;
        this.om = om;
    }

    public Game extractLastGame() {
        Long currentLoggedUserId = userService.getCurrentLoggedUserId();

        UserEntity currentLoggedUser = userService.getCurrentLoggedUser();
        Optional<GameEntity> byOwnerId = lastGameRepository.findByOwnerId(currentLoggedUserId);

        if (byOwnerId.isEmpty()) {
            Game game = new Game();

            try {
                save(game, currentLoggedUser);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            return game;
        }

        GameEntity gameEntity = byOwnerId.get();

        TypeReference<List<Card>> typeRef = new TypeReference<>() {
        };

        List<Card> dealerCards, playerCards;
        try {
            dealerCards = om.readValue(gameEntity.getDealerCards(), typeRef);
            playerCards = om.readValue(gameEntity.getPlayerCards(), typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new Game()
                .setHash(gameEntity.getHash())
                .setDealerCards(dealerCards)
                .setPlayerCards(playerCards)
                .setDealt(true);
    }

    private void save(Game game, UserEntity currentLoggedUser) throws JsonProcessingException {
        GameEntity gameEntity = new GameEntity()
                .setHash(game.getHash())
                .setDealerCards(om.writeValueAsString(game.getDealerCards()))
                .setPlayerCards(om.writeValueAsString(game.getPlayerCards()))
                .setOwner(currentLoggedUser);

        lastGameRepository.save(gameEntity);
    }


}
