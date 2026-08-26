package com.casino.blackjack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Push365App {

    public static void main(String[] args) {
        SpringApplication.run(Push365App.class, args);
    }
}
