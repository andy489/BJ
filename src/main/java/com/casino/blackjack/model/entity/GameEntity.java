package com.casino.blackjack.model.entity;

import com.casino.blackjack.service.gamelogic.dto.Game;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.PROP_IND_AVAILABLE_CHOICES_CARDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PROP_IND_DEALER_CARDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PROP_IND_DEALER_SECOND_CARD;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PROP_IND_ERR_CODE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PROP_IND_PLAYER_CARDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.PROP_IND_TAKEN_CHOICES;

@Entity
@Table(name = "last_games")
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true, exclude = {"owner"})
public class GameEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String hash;

    private String playerCards;

    private String dealerCards;

    private String availableChoices;

    private String takenChoices;

    private Boolean insurance;

    private Boolean doubleDown;

    private Double handMultiplier;

    private Double insuranceMultiplier;

    private String errCodeList;

    private String dealerSecondCard;

    private Boolean finalized;

    private LocalDateTime dealtTime;

    @OneToOne
    private UserEntity owner;

    public static GameEntity of(Game game, ObjectMapper om, UserEntity owner) {
        List<String> properties = extractGameProperties(game, om);

        return new GameEntity()
                .setHash(game.getHash())
                .setFinalized(game.getFinalized())
                .setDealerCards(properties.get(PROP_IND_DEALER_CARDS))
                .setPlayerCards(properties.get(PROP_IND_PLAYER_CARDS))
                .setAvailableChoices(properties.get(PROP_IND_AVAILABLE_CHOICES_CARDS))
                .setTakenChoices(properties.get(PROP_IND_TAKEN_CHOICES))
                .setErrCodeList(properties.get(PROP_IND_ERR_CODE))
                .setHandMultiplier(game.getHandMultiplier())
                .setInsuranceMultiplier(game.getInsuranceMultiplier())
                .setInsurance(game.getInsurance())
                .setDoubleDown(game.getDoubleDown())
                .setDealerSecondCard(properties.get(PROP_IND_DEALER_SECOND_CARD))
                .setDealtTime(game.getDealtTime())
                .setOwner(owner);
    }

    public static GameEntity map(GameEntity gameEntity, Game game, ObjectMapper om) {
        List<String> properties = extractGameProperties(game, om);

        return gameEntity
                .setHash(game.getHash())
                .setFinalized(game.getFinalized())
                .setDealerCards(properties.get(PROP_IND_DEALER_CARDS))
                .setPlayerCards(properties.get(PROP_IND_PLAYER_CARDS))
                .setAvailableChoices(properties.get(PROP_IND_AVAILABLE_CHOICES_CARDS))
                .setTakenChoices(properties.get(PROP_IND_TAKEN_CHOICES))
                .setErrCodeList(properties.get(PROP_IND_ERR_CODE))
                .setHandMultiplier(game.getHandMultiplier())
                .setInsuranceMultiplier(game.getInsuranceMultiplier())
                .setInsurance(game.getInsurance())
                .setDoubleDown(game.getDoubleDown())
                .setDealerSecondCard(properties.get(PROP_IND_DEALER_SECOND_CARD))
                .setDealtTime(game.getDealtTime());
    }

    private static List<String> extractGameProperties(Game game, ObjectMapper om) {
        List<String> properties = new ArrayList<>();

        try {
            properties.add(om.writeValueAsString(game.getDealerCards()));
            properties.add(om.writeValueAsString(game.getPlayerCards()));
            properties.add(om.writeValueAsString(game.getAvailableChoices()));
            properties.add(om.writeValueAsString(game.getTakenChoices()));
            properties.add(om.writeValueAsString(game.getErrCodeList()));
            properties.add(om.writeValueAsString(game.getDealerSecondCard()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public GameEntity makeChoice(Integer choice, ObjectMapper om) {
        try {
            List<Integer> currChoices = om.readValue(takenChoices, new TypeReference<>() {
            });

            currChoices.add(choice);

            this.takenChoices = om.writeValueAsString(currChoices);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this;
    }
}
