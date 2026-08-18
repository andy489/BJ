package com.casino.blackjack.service.gamelogic.rng.scenario;

import com.casino.blackjack.service.gamelogic.rng.CardSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class CardSourceConfig {

    // ── Set the active deck scenario here ─────────────────────────────────────
    private static final DeckScenario SCENARIO = DeckScenario.RANDOM;
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public CardSource cardSource(Environment env) {
        if (SCENARIO != DeckScenario.RANDOM && env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "FixedCardSource must not run in production — set SCENARIO to RANDOM");
        }
        return SCENARIO.source();
    }
}
