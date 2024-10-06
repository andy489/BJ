package com.casino.blackjack.model.entity;

import com.casino.blackjack.service.gamelogic.dto.Wallet;
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

    private BigDecimal handBet;

    private BigDecimal insuranceBet;

    @OneToOne
    private UserEntity owner;

    public WalletEntity() {
        balance = BigDecimal.ZERO;
        lastWin = BigDecimal.ZERO;
        lastBet = BigDecimal.ZERO;
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
    }

    public static WalletEntity of(Wallet wallet) {
        return new WalletEntity()
                .setBalance(wallet.getBalance())
                .setCurrentBet(wallet.getCurrentBet())
                .setLastWin(wallet.getLastBet())
                .setCurrentBet(wallet.getCurrentBet())
                .setHandBet(wallet.getHandBet())
                .setInsuranceBet(wallet.getInsuranceBet());
    }

    public static void map(WalletEntity walletEntity, Wallet wallet) {
        walletEntity.setBalance(wallet.getBalance())
                .setLastWin(wallet.getLastWin())
                .setLastBet(wallet.getLastBet())
                .setCurrentBet(wallet.getCurrentBet())
                .setHandBet(wallet.getHandBet())
                .setInsuranceBet(wallet.getInsuranceBet());
    }

    public BigDecimal deposit(BigDecimal depositSum) {
        balance = balance.add(depositSum);

        return balance;
    }

    // returns total bet amount
    public BigDecimal payBet(Double handMultiplier, Double insuranceMultiplier) {
        lastWin = handBet.multiply(new BigDecimal(handMultiplier))
                .add(insuranceBet.multiply(new BigDecimal(insuranceMultiplier)));

        balance = balance.add(lastWin);
        BigDecimal toReturn = new BigDecimal(String.valueOf(currentBet));
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;

        return toReturn;
    }

    public void placeHandBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        handBet = betValue;
        currentBet = currentBet.add(betValue);
    }

    public void placeInsuranceBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        insuranceBet = betValue;
        currentBet = currentBet.add(betValue);
    }
}
