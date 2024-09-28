package com.casino.blackjack.repo;

import com.casino.blackjack.model.entity.PastGameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PastGameRepository extends JpaRepository<PastGameEntity, Long> {

    List<PastGameEntity> findByOwnerId(Long ownerId);

    List<Integer> findByHash(String gameHash);
}
