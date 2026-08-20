package com.casino.blackjack.service.gamelogic.rng.scenario;

import com.casino.blackjack.config.GameProperties;
import com.casino.blackjack.service.gamelogic.rng.CardSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class CardSourceConfig {

  @Bean
  public CardSource cardSource(GameProperties props, Environment env) {
    DeckScenario scenario = props.getDeckScenario();
    if (scenario != DeckScenario.RANDOM && env.acceptsProfiles(Profiles.of("prod"))) {
      throw new IllegalStateException(
          "FixedCardSource must not run in production — set game.blackjack.deck-scenario=RANDOM");
    }
    return scenario.source();
  }
}
