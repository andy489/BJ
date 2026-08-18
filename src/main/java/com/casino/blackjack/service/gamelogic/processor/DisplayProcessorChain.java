package com.casino.blackjack.service.gamelogic.processor;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processor chain for GET /play (display only).
 * Only runs processors that are safe to execute on every page load:
 * - FinalizedPayoutProcessor: pays out and archives a completed game
 * - ErrorPassthroughProcessor: clears one-shot error codes after they are shown
 *
 * Card-dealing processors (Hit, Stand, DoubleDown, etc.) are intentionally
 * excluded — they run exclusively during POST actions via GameStateProcessorChain.
 */
@Component
public class DisplayProcessorChain {

    private final List<GameStateProcessor> processors = List.of(
            new FinalizedPayoutProcessor(),
            new ErrorPassthroughProcessor()
    );

    public GameContext process(GameContext ctx) {
        for (GameStateProcessor processor : processors) {
            if (processor.canProcess(ctx)) {
                return processor.process(ctx);
            }
        }
        return ctx;
    }
}
