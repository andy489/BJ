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
import java.util.ArrayList;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.Util.AVAILABLE_CHOICES_CARDS_PROP_IND;
import static com.casino.blackjack.service.gamelogic.util.Util.DEALER_CARDS_PROP_IND;
import static com.casino.blackjack.service.gamelogic.util.Util.ERR_CODE_PROP_IND;
import static com.casino.blackjack.service.gamelogic.util.Util.PLAYER_CARDS_PROP_IND;
import static com.casino.blackjack.service.gamelogic.util.Util.TAKEN_CHOICES_PROP_IND;

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

    private Double winMultiplier;

    private Boolean insurance;

    private Boolean secondDealerCardTen;

    private String errCodeList;

    private Boolean finalized;

    @OneToOne
    private UserEntity owner;

    public static GameEntity of(Game game, ObjectMapper om, UserEntity owner) {
        List<String> properties = extractGameProperties(game, om);

        return new GameEntity()
                .setHash(game.getHash())
                .setFinalized(game.getFinalized())
                .setDealerCards(properties.get(DEALER_CARDS_PROP_IND))
                .setPlayerCards(properties.get(PLAYER_CARDS_PROP_IND))
                .setAvailableChoices(properties.get(AVAILABLE_CHOICES_CARDS_PROP_IND))
                .setTakenChoices(properties.get(TAKEN_CHOICES_PROP_IND))
                .setErrCodeList(properties.get(ERR_CODE_PROP_IND))
                .setWinMultiplier(game.getWinMultiplier())
                .setInsurance(game.getInsurance())
                .setSecondDealerCardTen(game.getSecondDealerCardTen())
                .setOwner(owner);
    }

    public static GameEntity map(GameEntity gameEntity, Game game, ObjectMapper om) {
        List<String> properties = extractGameProperties(game, om);

        return gameEntity
                .setHash(game.getHash())
                .setFinalized(game.getFinalized())
                .setDealerCards(properties.get(DEALER_CARDS_PROP_IND))
                .setPlayerCards(properties.get(PLAYER_CARDS_PROP_IND))
                .setAvailableChoices(properties.get(AVAILABLE_CHOICES_CARDS_PROP_IND))
                .setTakenChoices(properties.get(TAKEN_CHOICES_PROP_IND))
                .setErrCodeList(properties.get(ERR_CODE_PROP_IND))
                .setWinMultiplier(game.getWinMultiplier())
                .setInsurance(game.getInsurance())
                .setSecondDealerCardTen(game.getSecondDealerCardTen());
    }

    private static List<String> extractGameProperties(Game game, ObjectMapper om) {
        List<String> properties = new ArrayList<>();

        try {
            properties.add(om.writeValueAsString(game.getDealerCards()));
            properties.add(om.writeValueAsString(game.getPlayerCards()));
            properties.add(om.writeValueAsString(game.getAvailableChoices()));
            properties.add(om.writeValueAsString(game.getTakenChoices()));
            properties.add(om.writeValueAsString(game.getErrCodeList()));
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
