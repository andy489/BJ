package com.casino.blackjack.service.gamelogic.dto;

import com.casino.blackjack.model.entity.WalletEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Wallet {

    private BigDecimal balance;

    private BigDecimal lastWin;

    private BigDecimal lastBet;

    private BigDecimal currentBet;

    public Wallet() {
        balance = BigDecimal.ZERO;
        lastWin = BigDecimal.ZERO;
        currentBet = BigDecimal.ZERO;
    }

    public static Wallet of(WalletEntity walletEntity) {
        return new Wallet()
                .setBalance(walletEntity.getBalance())
                .setLastWin(walletEntity.getLastWin())
                .setCurrentBet(walletEntity.getCurrentBet());
    }

    public static WalletEntity map(WalletEntity walletEntity, Wallet wallet, ObjectMapper om) {
        return walletEntity
                .setBalance(wallet.getBalance())
                .setLastWin(wallet.getLastWin())
                .setCurrentBet(wallet.getCurrentBet());
    }

    public Wallet deposit(BigDecimal depositSum) {
        balance = balance.add(depositSum);
        return this;
    }

    public Wallet payBet(Double multiplier) {
        lastWin = currentBet.multiply(new BigDecimal(multiplier));
        balance = balance.add(lastWin);
        currentBet = BigDecimal.ZERO;
        return this;
    }

    public Wallet placeBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = betValue;
        return this;
    }
}
