package com.casino.blackjack;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERRORS;

@SpringBootApplication
@EnableScheduling
public class BlackjackApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BlackjackApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println(CHOICES);
        System.out.println(ERRORS);
    }
}
