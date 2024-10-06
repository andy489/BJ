package com.casino.blackjack.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "bet_history")
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true, exclude = {})
public class BetHistoryEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(nullable = false)
    private BigDecimal totalBetAmount;

    @Column(nullable = false)
    private BigDecimal returnAmount;

    @ManyToOne(cascade = {CascadeType.MERGE})
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "game_hash", referencedColumnName = "hash")
    private PlayedGameEntity playedGame;

    public static BetHistoryEntity of(BigDecimal totalBetAmount, BigDecimal returnAmount,
                                      UserEntity user, PlayedGameEntity playedGame) {

        return new BetHistoryEntity()
                .setTotalBetAmount(BigDecimal.ZERO)
                .setReturnAmount(BigDecimal.ZERO)
                .setUser(user)
                .setPlayedGame(playedGame);
    }
}
