package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.repo.WalletRepository;
import com.casino.blackjack.service.auth.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public boolean cashOut(String cashOutSum, Long ownerId) {
        String formatted = cashOutSum.replaceAll(",", "");
        BigDecimal amount = new BigDecimal(formatted);

        Optional<WalletEntity> walletOpt = walletRepository.getReferenceByOwnerId(ownerId);
        if (walletOpt.isEmpty()) return false;

        WalletEntity wallet = walletOpt.get();
        if (wallet.getBalance().compareTo(amount) < 0) return false;

        wallet.cashOut(amount);
        walletRepository.save(wallet);
        return true;
    }

    public BigDecimal getBalance(Long ownerId) {
        return walletRepository.getReferenceByOwnerId(ownerId)
                .map(WalletEntity::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}
