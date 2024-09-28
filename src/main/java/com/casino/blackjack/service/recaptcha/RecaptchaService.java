package com.casino.blackjack.service.recaptcha;

import com.casino.blackjack.config.recaptcha.RecaptchaConfig;
import com.casino.blackjack.model.dto.RecaptchaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Optional;

@Service
public class RecaptchaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecaptchaService.class);

    private final WebClient webClient;

    private final RecaptchaConfig recaptchaConfig;

    public RecaptchaService(WebClient webClient, RecaptchaConfig recaptchaConfig) {
        this.webClient = webClient;
        this.recaptchaConfig = recaptchaConfig;
    }

    public Optional<RecaptchaResponseDTO> verify(String token) {
        if (recaptchaConfig.getEnabled()) {
            return Optional.ofNullable(webClient.post()
                    .uri(this::buildReCaptchaURI)
                    .body(BodyInserters.fromFormData("secret", recaptchaConfig.getSecret())
                            .with("response", token))
                    .retrieve()
                    .bodyToMono(RecaptchaResponseDTO.class)
                    .doOnError(t -> LOGGER.error("Error fetching google response...", t))
                    .onErrorComplete()
                    .block());
        }

        return Optional.of(new RecaptchaResponseDTO().setSuccess(true));
    }

    private URI buildReCaptchaURI(UriBuilder uriBuilder) {
        // REST endpoint for google verification
        // https://www.google.com/recaptcha/api/siteverify

        return uriBuilder
                .scheme("https")
                .host("www.google.com")
                .path("/recaptcha/api/siteverify")
                .build();
    }
}
