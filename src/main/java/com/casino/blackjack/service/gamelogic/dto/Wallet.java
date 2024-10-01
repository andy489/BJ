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
}
