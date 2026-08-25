package com.casino.blackjack.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;

@Entity
@Table(name = "activation_tokens")
@Getter
@Setter
@Accessors(chain = true)
public class UserActivationTokenEntity extends BaseEntity {

    @OneToOne
    private UserEntity user;

    private String token;

    @Column(columnDefinition = "BIGINT")
    private Instant createdAt;
}
