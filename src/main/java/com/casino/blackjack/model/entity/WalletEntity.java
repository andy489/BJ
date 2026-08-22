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

    private BigDecimal doubleBet;

    private BigDecimal insuranceBet;

    private BigDecimal splitBet;

    private BigDecimal perfectPairsBet;

    private BigDecimal twentyOneThreeBet;

    private BigDecimal dealerPerfectPairsBet;

    @OneToOne
    private UserEntity owner;

    public WalletEntity() {
        balance = BigDecimal.ZERO;
        lastWin = BigDecimal.ZERO;
        lastBet = BigDecimal.ZERO;
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        doubleBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
        splitBet = BigDecimal.ZERO;
        perfectPairsBet = BigDecimal.ZERO;
        twentyOneThreeBet = BigDecimal.ZERO;
        dealerPerfectPairsBet = BigDecimal.ZERO;
    }

    public static WalletEntity of(Wallet wallet) {
        return new WalletEntity()
                .setBalance(wallet.getBalance())
                .setCurrentBet(wallet.getCurrentBet())
                .setLastWin(wallet.getLastWin())
                .setLastBet(wallet.getLastBet())
                .setHandBet(wallet.getHandBet())
                .setDoubleBet(wallet.getDoubleBet())
                .setInsuranceBet(wallet.getInsuranceBet())
                .setSplitBet(wallet.getSplitBet())
                .setPerfectPairsBet(wallet.getPerfectPairsBet())
                .setTwentyOneThreeBet(wallet.getTwentyOneThreeBet())
                .setDealerPerfectPairsBet(wallet.getDealerPerfectPairsBet());
    }

    public static void map(WalletEntity walletEntity, Wallet wallet) {
        walletEntity.setBalance(wallet.getBalance())
                .setLastWin(wallet.getLastWin())
                .setLastBet(wallet.getLastBet())
                .setCurrentBet(wallet.getCurrentBet())
                .setHandBet(wallet.getHandBet())
                .setDoubleBet(wallet.getDoubleBet())
                .setInsuranceBet(wallet.getInsuranceBet())
                .setSplitBet(wallet.getSplitBet())
                .setPerfectPairsBet(wallet.getPerfectPairsBet())
                .setTwentyOneThreeBet(wallet.getTwentyOneThreeBet())
                .setDealerPerfectPairsBet(wallet.getDealerPerfectPairsBet());
    }

    public BigDecimal deposit(BigDecimal depositSum) {
        balance = balance.add(depositSum);
        return balance;
    }

    public BigDecimal cashOut(BigDecimal amount) {
        balance = balance.subtract(amount);
        return balance;
    }

    // returns total bet amount
    public BigDecimal payBet(Double handMultiplier, Double insuranceMultiplier) {
        BigDecimal totalReturn = handBet.multiply(new BigDecimal(handMultiplier))
                .add(doubleBet.multiply(new BigDecimal(handMultiplier)))
                .add(insuranceBet.multiply(new BigDecimal(insuranceMultiplier)));
        lastWin = totalReturn.max(BigDecimal.ZERO);
        lastBet = new BigDecimal(String.valueOf(currentBet));

        balance = balance.add(totalReturn);
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        doubleBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
        splitBet = BigDecimal.ZERO;
        perfectPairsBet = BigDecimal.ZERO;
        twentyOneThreeBet = BigDecimal.ZERO;
        dealerPerfectPairsBet = BigDecimal.ZERO;

        return lastBet;
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
