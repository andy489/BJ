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

    private BigDecimal handBet;

    private BigDecimal insuranceBet;

    public Wallet() {
        balance = BigDecimal.ZERO;
        lastWin = BigDecimal.ZERO;
        lastBet = BigDecimal.ZERO;
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
    }

    public static Wallet of(WalletEntity walletEntity) {
        return new Wallet()
                .setBalance(walletEntity.getBalance())
                .setLastWin(walletEntity.getLastWin())
                .setCurrentBet(walletEntity.getCurrentBet())
                .setHandBet(walletEntity.getHandBet())
                .setInsuranceBet(walletEntity.getInsuranceBet());
    }

    public static WalletEntity map(WalletEntity walletEntity, Wallet wallet) {
        return walletEntity
                .setBalance(wallet.getBalance())
                .setLastWin(wallet.getLastWin())
                .setCurrentBet(wallet.getCurrentBet())
                .setHandBet(wallet.getHandBet())
                .setInsuranceBet(wallet.getInsuranceBet());
    }

    public Wallet deposit(BigDecimal depositSum) {
        balance = balance.add(depositSum);
        return this;
    }

    public Wallet payBet(Double handMultiplier, Double insuranceMultiplier) {
        lastWin = handBet.multiply(new BigDecimal(handMultiplier))
                .add(insuranceBet.multiply(new BigDecimal(insuranceMultiplier)));

        balance = balance.add(lastWin);
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
        return this;
    }

    public Wallet placeHandBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = currentBet.add(betValue);
        handBet = betValue;
        return this;
    }

    public Wallet placeInsurance(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = currentBet.add(betValue);
        insuranceBet = betValue;
        return this;
    }
}
