package com.casino.blackjack.service.gamelogic.processor;

public interface GameStateProcessor {

    boolean canProcess(GameContext ctx);

    GameContext process(GameContext ctx);
}
