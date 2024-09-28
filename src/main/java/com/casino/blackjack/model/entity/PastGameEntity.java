package com.casino.blackjack.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "past_games")
@Getter
@Setter
@Accessors(chain = true)
public class PastGameEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String hash;

    private String playerCards;

    private String dealerCards;

    private Double winMultiplier;

    @ManyToOne(cascade = {CascadeType.MERGE})
    private UserEntity owner;

    public static PastGameEntity of(GameEntity gameEntity) {
        return new PastGameEntity()
                .setHash(gameEntity.getHash())
                .setPlayerCards(gameEntity.getPlayerCards())
                .setDealerCards(gameEntity.getDealerCards())
                .setWinMultiplier(gameEntity.getWinMultiplier())
                .setOwner(gameEntity.getOwner());
    }
}
