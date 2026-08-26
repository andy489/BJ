package com.casino.blackjack.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RenderKeepAliveScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderKeepAliveScheduler.class);

    private final WebClient webClient;
    private final String appUrl;

    public RenderKeepAliveScheduler(@Qualifier("jsonWebClient") WebClient webClient,
                                    @Value("${RENDER_EXTERNAL_URL:https://push365.onrender.com}") String appUrl) {
        this.webClient = webClient;
        this.appUrl = appUrl;
    }

    // Every 10 minutes — Render free tier sleeps after ~15 min of inactivity
    @Scheduled(fixedDelayString = "${keep-alive.interval-ms:600000}",
               initialDelayString = "${keep-alive.initial-delay-ms:30000}")
    public void ping() {
        if (appUrl == null || appUrl.isBlank()) {
            return;
        }

        String url = appUrl.stripTrailing() + "/keep-alive";
        try {
            String status = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(t -> LOGGER.warn("[KeepAlive] ping failed: {}", t.getMessage()))
                    .onErrorReturn("error")
                    .block();

            if ("error".equals(status)) {
                LOGGER.warn("[KeepAlive] ping to {} returned error", url);
            } else {
                LOGGER.info("[KeepAlive] ping ok -> {}", url);
            }
        } catch (Exception e) {
            LOGGER.warn("[KeepAlive] ping exception: {}", e.getMessage());
        }
    }
}
