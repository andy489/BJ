package com.casino.blackjack.model.entity;

import com.casino.blackjack.service.gamelogic.dto.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class WalletEntity extends BaseEntity {

    private BigDecimal balance;

    private BigDecimal currentBet;

    private BigDecimal lastBet;

    private BigDecimal lastWin;

    @OneToOne
    private UserEntity owner;

    public WalletEntity() {
        balance = BigDecimal.ZERO;
        currentBet = BigDecimal.ZERO;
        lastBet = BigDecimal.ZERO;
        lastWin = BigDecimal.ZERO;
    }

    public static WalletEntity of(Wallet wallet) {
        return new WalletEntity()
                .setBalance(wallet.getBalance())
                .setCurrentBet(wallet.getCurrentBet())
                .setLastWin(wallet.getLastBet())
                .setCurrentBet(wallet.getCurrentBet());
    }

    public static void map(WalletEntity walletEntity, Wallet wallet) {
        walletEntity.setBalance(wallet.getBalance())
                .setLastWin(wallet.getLastWin())
                .setLastBet(wallet.getLastBet())
                .setCurrentBet(wallet.getCurrentBet());
    }

    public BigDecimal deposit(BigDecimal depositSum) {
        balance = balance.add(depositSum);

        return balance;
    }

    public void payBet(Double multiplier) {
        lastWin = currentBet.multiply(new BigDecimal(multiplier));
        balance = balance.add(lastWin);
        lastBet = currentBet;
        currentBet = BigDecimal.ZERO;
    }

    public void placeBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = betValue;
    }
}
