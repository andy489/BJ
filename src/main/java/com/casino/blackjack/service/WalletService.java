package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.repo.WalletRepository;
import com.casino.blackjack.service.auth.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Optional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    private final UserService userService;

    public WalletService(WalletRepository walletRepository, UserService userService) {
        this.walletRepository = walletRepository;
        this.userService = userService;
    }

    public void deposit(String depositSum, Long ownerId) {

        Optional<WalletEntity> walletById = walletRepository.getReferenceByOwnerId(ownerId);

        String depositSumFormatted = depositSum.replaceAll(",", "");;

        if (walletById.isEmpty()) {

            Optional<UserEntity> byId = userService.findById(ownerId);

            if (byId.isEmpty()) {
                throw new IllegalStateException("ERR: no logged user");
            }

            WalletEntity walletEntity = new WalletEntity()
                    .setBalance(new BigDecimal(depositSumFormatted))
                    .setOwner(byId.get())
                    .setLastWin(BigDecimal.ZERO)
                    .setCurrentBet(BigDecimal.ZERO);

            walletRepository.save(walletEntity);

            return;
        }

        WalletEntity walletEntity = walletById.get();
        walletEntity.deposit(new BigDecimal(depositSumFormatted));
        walletRepository.save(walletEntity);
    }
}
