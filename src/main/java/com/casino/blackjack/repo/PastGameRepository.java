package com.casino.blackjack.repo;

import com.casino.blackjack.model.entity.PlayedGameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PastGameRepository extends JpaRepository<PlayedGameEntity, Long> {

    List<PlayedGameEntity> findByOwnerId(Long ownerId);

    List<Integer> findByHash(String gameHash);
}
