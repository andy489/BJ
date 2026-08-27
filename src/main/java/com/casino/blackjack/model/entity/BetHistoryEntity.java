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
    private Boolean doubleDown;

    @Column(nullable = false)
    private Boolean split;

    @Column(nullable = false)
    private BigDecimal returnAmount;

    @Column(nullable = false)
    private BigDecimal ppBet = BigDecimal.ZERO;

    @Column(name = "t3_bet", nullable = false)
    private BigDecimal t3Bet = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal dppBet = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal ppWin = BigDecimal.ZERO;

    @Column(name = "t3_win", nullable = false)
    private BigDecimal t3Win = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal dppWin = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal handBet = BigDecimal.ZERO;

    @ManyToOne(cascade = {CascadeType.MERGE})
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "game_hash", referencedColumnName = "hash")
    private PlayedGameEntity playedGame;
}
