package com.casino.blackjack.service.gamelogic.rng;

import org.apache.commons.math3.random.MersenneTwister;

import static com.casino.blackjack.service.gamelogic.util.Util.KING_RANK;
import static com.casino.blackjack.service.gamelogic.util.Util.SPADES_SUIT;
import static java.lang.Math.abs;

public class RNG {

    private static final String GAME_HASH_SYM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXY0123456789";

    private static final Integer GAME_HASH_LEN = 10;

    private static final MersenneTwister mersenneTwister = new MersenneTwister();

    public static Integer nextInt() {
        return abs(mersenneTwister.nextInt());
    }

    public static Integer randSuit() {
        return nextInt() % (SPADES_SUIT + 1);
    }

    public static Integer randRank() {
        return nextInt() % KING_RANK + 1;
    }

    public static String generateGameHash() {

        StringBuilder hash = new StringBuilder();

        for (int i = 0; i < GAME_HASH_LEN; i++) {
            int randInd = RNG.nextInt() % GAME_HASH_SYM.length();

            hash.append(GAME_HASH_SYM.charAt(randInd));
        }

        return hash.toString();
    }
}
