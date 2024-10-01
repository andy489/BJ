package com.casino.blackjack.model.entity;

import com.casino.blackjack.service.gamelogic.dto.Game;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "played_games")
@Getter
@Setter
@Accessors(chain = true)
public class PlayedGameEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String hash;

    private String playerCards;

    private String dealerCards;

    private String takenChoices;

    private Double winMultiplier;

    private Boolean insurance;

    private Boolean secondDealerCardTen;

    @ManyToOne(cascade = {CascadeType.MERGE})
    private UserEntity owner;

    public static PlayedGameEntity of(GameEntity gameEntity) {
        return new PlayedGameEntity()
                .setHash(gameEntity.getHash())
                .setPlayerCards(gameEntity.getPlayerCards())
                .setDealerCards(gameEntity.getDealerCards())
                .setTakenChoices(gameEntity.getTakenChoices())
                .setWinMultiplier(gameEntity.getWinMultiplier())
                .setInsurance(gameEntity.getInsurance())
                .setSecondDealerCardTen(gameEntity.getSecondDealerCardTen())
                .setOwner(gameEntity.getOwner());
    }

    public static PlayedGameEntity of(Game game, ObjectMapper om, UserEntity owner) {

        String dealerCards, playerCards, takenChoices;

        try {
            dealerCards = om.writeValueAsString(game.getDealerCards());
            playerCards = om.writeValueAsString(game.getPlayerCards());
            takenChoices = om.writeValueAsString(game.getTakenChoices());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new PlayedGameEntity()
                .setHash(game.getHash())
                .setDealerCards(dealerCards)
                .setPlayerCards(playerCards)
                .setTakenChoices(takenChoices)
                .setWinMultiplier(game.getWinMultiplier())
                .setInsurance(game.getInsurance())
                .setSecondDealerCardTen(game.getSecondDealerCardTen())
                .setOwner(owner);
    }
}
