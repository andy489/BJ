package com.casino.blackjack.config.recaptcha;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.recaptcha")
@Getter
@Setter
@Accessors(chain = true)
public class RecaptchaConfig {

    private Boolean enabled;

    private String site;

    private String secret;
}
