package com.casino.blackjack.service.gamelogic.dto;

import com.casino.blackjack.model.entity.WalletEntity;
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

    private BigDecimal lastHandWin;

    private BigDecimal lastPpWin;

    private BigDecimal lastT3Win;

    private BigDecimal lastDppWin;

    private BigDecimal lastBet;

    private BigDecimal lastTotalBet;

    private BigDecimal lastPpBet;

    private BigDecimal lastT3Bet;

    private BigDecimal lastDppBet;

    private BigDecimal currentBet;

    private BigDecimal handBet;

    private BigDecimal doubleBet;

    private BigDecimal insuranceBet;

    private BigDecimal splitBet;

    private BigDecimal perfectPairsBet;

    private BigDecimal twentyOneThreeBet;

    private BigDecimal dealerPerfectPairsBet;

    public Wallet() {
        balance = BigDecimal.ZERO;
        lastWin = BigDecimal.ZERO;
        lastHandWin = BigDecimal.ZERO;
        lastBet = BigDecimal.ZERO;
        lastTotalBet = BigDecimal.ZERO;
        lastPpBet = BigDecimal.ZERO;
        lastT3Bet = BigDecimal.ZERO;
        lastDppBet = BigDecimal.ZERO;
        lastPpWin = BigDecimal.ZERO;
        lastT3Win = BigDecimal.ZERO;
        lastDppWin = BigDecimal.ZERO;
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        doubleBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
        splitBet = BigDecimal.ZERO;
        perfectPairsBet = BigDecimal.ZERO;
        twentyOneThreeBet = BigDecimal.ZERO;
        dealerPerfectPairsBet = BigDecimal.ZERO;
    }

    public static Wallet of(WalletEntity walletEntity) {
        return new Wallet()
                .setBalance(walletEntity.getBalance())
                .setLastWin(walletEntity.getLastWin())
                .setLastHandWin(nvl(walletEntity.getLastHandWin()))
                .setLastPpWin(walletEntity.getLastPpWin())
                .setLastT3Win(walletEntity.getLastT3Win())
                .setLastDppWin(walletEntity.getLastDppWin())
                .setLastBet(walletEntity.getLastBet())
                .setLastTotalBet(walletEntity.getLastTotalBet())
                .setLastPpBet(nvl(walletEntity.getLastPpBet()))
                .setLastT3Bet(nvl(walletEntity.getLastT3Bet()))
                .setLastDppBet(nvl(walletEntity.getLastDppBet()))
                .setCurrentBet(walletEntity.getCurrentBet())
                .setHandBet(walletEntity.getHandBet())
                .setInsuranceBet(walletEntity.getInsuranceBet())
                .setDoubleBet(walletEntity.getDoubleBet())
                .setSplitBet(walletEntity.getSplitBet())
                .setPerfectPairsBet(walletEntity.getPerfectPairsBet())
                .setTwentyOneThreeBet(walletEntity.getTwentyOneThreeBet())
                .setDealerPerfectPairsBet(walletEntity.getDealerPerfectPairsBet());
    }

    public static WalletEntity map(WalletEntity walletEntity, Wallet wallet) {
        return walletEntity
                .setBalance(wallet.getBalance())
                .setLastWin(wallet.getLastWin())
                .setLastHandWin(wallet.getLastHandWin())
                .setLastPpWin(wallet.getLastPpWin())
                .setLastT3Win(wallet.getLastT3Win())
                .setLastDppWin(wallet.getLastDppWin())
                .setLastBet(wallet.getLastBet())
                .setLastTotalBet(wallet.getLastTotalBet())
                .setLastPpBet(wallet.getLastPpBet())
                .setLastT3Bet(wallet.getLastT3Bet())
                .setLastDppBet(wallet.getLastDppBet())
                .setCurrentBet(wallet.getCurrentBet())
                .setHandBet(wallet.getHandBet())
                .setDoubleBet(wallet.getDoubleBet())
                .setInsuranceBet(wallet.getInsuranceBet())
                .setSplitBet(wallet.getSplitBet())
                .setPerfectPairsBet(wallet.getPerfectPairsBet())
                .setTwentyOneThreeBet(wallet.getTwentyOneThreeBet())
                .setDealerPerfectPairsBet(wallet.getDealerPerfectPairsBet());
    }

    public Wallet deposit(BigDecimal depositSum) {
        balance = balance.add(depositSum);
        return this;
    }

    public Wallet payBet(Double handMultiplier, Double insuranceMultiplier) {
        BigDecimal totalReturn = handBet.multiply(new BigDecimal(handMultiplier))
                .add(insuranceBet.multiply(new BigDecimal(insuranceMultiplier)));
        // lastWin = net profit (0 on push, negative shows as 0 implicitly via display)
        lastWin = totalReturn.subtract(handBet).subtract(insuranceBet);

        balance = balance.add(totalReturn);
        currentBet = BigDecimal.ZERO;
        handBet = BigDecimal.ZERO;
        insuranceBet = BigDecimal.ZERO;
        return this;
    }

    public Wallet placeHandBet(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = betValue;
        handBet = betValue;
        return this;
    }

    public Wallet placeInsurance(BigDecimal betValue) {
        balance = balance.subtract(betValue);
        currentBet = currentBet.add(betValue);
        insuranceBet = betValue;
        return this;
    }

    public boolean cannotAffordDouble() {
        return balance.compareTo(currentBet) < 0;
    }

    public void doubleBet() {
        balance = balance.subtract(currentBet);
        currentBet = currentBet.add(currentBet);
        doubleBet = handBet;
    }

    public BigDecimal totalStake() {
        BigDecimal pp  = perfectPairsBet  != null ? perfectPairsBet  : BigDecimal.ZERO;
        BigDecimal t3  = twentyOneThreeBet != null ? twentyOneThreeBet : BigDecimal.ZERO;
        BigDecimal dpp = dealerPerfectPairsBet != null ? dealerPerfectPairsBet : BigDecimal.ZERO;
        return currentBet.add(pp).add(t3).add(dpp);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
