package com.casino.blackjack.service.gamelogic.processor;

import com.casino.blackjack.config.GameProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameStateProcessorChain {

    private final List<GameStateProcessor> processors;

    public GameStateProcessorChain(GameProperties gameProperties) {
        int maxSplits = gameProperties.getMaxSplits();
        processors = List.of(
                // ── Wallet/persistence layer (run first — these read/write DB) ──────────
                new RepeatLastBetProcessor(),
                new RepeatLastBetAgainProcessor(),
                new ClearLastBetProcessor(),
                new InsuranceBetProcessor(),
                new SplitBetProcessor(maxSplits),
                new DoubleDownBetProcessor(),
                new InsufficientFundsReCheckProcessor(),
                new ErrorPassthroughProcessor(),
                new FinalizedPayoutProcessor(),
                // ── Pure game logic (mutate Game only) ───────────────────────────────────
                new NotDealtOrFinalizedProcessor(),
                new DoubleDownConfirmProcessor(),
                new SurrenderProcessor(),
                new PlayerBlackjackAfterDealProcessor(),
                new EvenMoneyProcessor(),
                new HitProcessor(),
                new StandProcessor(),
                new AutoFinalizeProcessor(),
                new AutoPlayProcessor(),
                new SplitDdAdvanceProcessor(maxSplits),
                new InsuranceProcessor(),
                new InitialDealSetupProcessor()
        );
    }

    public GameContext process(GameContext ctx) {
        for (GameStateProcessor processor : processors) {
            if (processor.canProcess(ctx)) {
                return processor.process(ctx);
            }
        }
        return ctx;
    }
}
