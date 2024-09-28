package com.casino.blackjack.repo;

import com.casino.blackjack.model.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LastGameRepository extends JpaRepository<GameEntity, Long> {

    Optional<GameEntity> findByOwnerId(Long ownerId);
}
