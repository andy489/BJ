package com.casino.blackjack.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@Accessors(chain = true)
public class WalletEntity extends BaseEntity{

    private BigDecimal balance;

    private BigDecimal currentBet;

    private BigDecimal lastWin;

    @OneToOne
    private UserEntity owner;

    public BigDecimal deposit(BigDecimal depositSum){
        balance = balance.add(depositSum);

        return balance;
    }

    public void payBet(Double multiplier) {
        lastWin = currentBet.multiply(new BigDecimal(multiplier));
        balance = balance.add(lastWin);
        currentBet = BigDecimal.ZERO;
    }

    public void placeBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = betValue;
    }
}
