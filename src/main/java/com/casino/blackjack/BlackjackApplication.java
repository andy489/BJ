package com.casino.blackjack;

import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.casino.blackjack.service.gamelogic.util.GameUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.reflect.Field;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERRORS;

@SpringBootApplication
@EnableScheduling
public class BlackjackApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BlackjackApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println(ERRORS);
    }
}
