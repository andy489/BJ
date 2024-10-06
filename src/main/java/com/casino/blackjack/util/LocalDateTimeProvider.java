package com.casino.blackjack.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LocalDateTimeProvider {

    public LocalDateTime getTimeBeforeMinutes(int minutes) {

        return LocalDateTime.now().minusMinutes(minutes);
    }

    public LocalDateTime getNow() {
        return LocalDateTime.now();
    }
}
