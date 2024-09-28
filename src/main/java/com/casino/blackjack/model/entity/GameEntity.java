package com.casino.blackjack.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "last_games")
@Getter
@Setter
@Accessors(chain = true)
public class GameEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String playerCards;

    private String dealerCards;

    private String hash;

    @OneToOne
    private UserEntity owner;
}
