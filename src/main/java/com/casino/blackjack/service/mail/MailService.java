package com.casino.blackjack.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final TemplateEngine templateEngine;

    private final MessageSource messageSource;

    private final WebClient webClient;

    private final String appMail;

    private final String appBaseUrl;

    private final String resendApiKey;

    public MailService(TemplateEngine templateEngine,
                       MessageSource messageSource,
                       @Qualifier("jsonWebClient") WebClient webClient,
                       @Value("${mail.app-mail}") String appMail,
                       @Value("${RENDER_EXTERNAL_URL:http://localhost:8080}") String appBaseUrl,
                       @Value("${RESEND_API_KEY:}") String resendApiKey) {

        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.webClient = webClient;
        this.appMail = appMail;
        this.appBaseUrl = appBaseUrl.stripTrailing().replaceAll("/$", "");
        this.resendApiKey = resendApiKey;
    }

    public void sendRegistrationEmail(String email, String username, String fullName, Locale locale, String token) {
        String subject = getEmailActivationSubject(locale);
        String html = generateMessageContentActivation(locale, username, fullName, token);
        send(email, subject, html);
    }

    public void sendForgotPassEmail(String email, String username, String fullName, Locale locale, String token) {
        String subject = getEmailForgotPassSubject(locale);
        String html = generateMessageContentForgotPass(locale, username, fullName, token);
        send(email, subject, html);
    }

    public String previewActivationEmail(Locale locale) {
        return generateMessageContentActivation(locale, "johndoe", "John Doe", "preview-token-00000000");
    }

    public String previewForgotPassEmail(Locale locale) {
        return generateMessageContentForgotPass(locale, "johndoe", "John Doe", "preview-token-00000000");
    }

    private void send(String to, String subject, String html) {
        Map<String, Object> body = Map.of(
                "from", appMail,
                "to", List.of(to),
                "subject", subject,
                "html", html
        );

        webClient.post()
                .uri(RESEND_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(resp -> log.info("Resend accepted email to {}: {}", to, resp))
                .doOnError(e -> log.error("Resend rejected email to {}: {}", to, e.getMessage()))
                .subscribe(); // fire-and-forget — don't block the request thread
    }

    private String getEmailActivationSubject(Locale locale) {
        return messageSource.getMessage("email.activation.subject", new Object[0], locale);
    }

    private String getEmailForgotPassSubject(Locale locale) {
        return messageSource.getMessage("email.forgot.pass.subject", new Object[0], locale);
    }

    private String generateMessageContentActivation(Locale locale, String username, String fullName,
                                                    String activationToken) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("fullName", fullName);
        context.setVariable("token", activationToken);
        context.setVariable("rulesLink", "rules");
        context.setVariable("appBaseUrl", appBaseUrl);
        context.setVariable("currentYear", Year.now().getValue());
        context.setLocale(locale);

        return templateEngine.process("email/registration-activate", context);
    }

    private String generateMessageContentForgotPass(Locale locale, String username, String fullName,
                                                    String activationToken) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("fullName", fullName);
        context.setVariable("token", activationToken);
        context.setVariable("appBaseUrl", appBaseUrl);
        context.setVariable("currentYear", Year.now().getValue());
        context.setLocale(locale);

        return templateEngine.process("email/reset-pass", context);
    }
}
