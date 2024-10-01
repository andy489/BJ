package com.casino.blackjack.repo;

import com.casino.blackjack.model.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {
    Optional<WalletEntity> getReferenceByOwnerId(Long ownerId);

    Optional<WalletEntity> findByOwnerId(Long ownerId);
}
