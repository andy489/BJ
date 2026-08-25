package com.casino.blackjack.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.Locale;

@Service
public class MailService {

    private final TemplateEngine templateEngine;

    private final MessageSource messageSource;

    private final JavaMailSender javaMailSender;

    private final String appMail;

    private final String appBaseUrl;

    public MailService(TemplateEngine templateEngine,
                       MessageSource messageSource,
                       JavaMailSender javaMailSender,
                       @Value("${mail.app-mail}") String appMail,
                       @Value("${RENDER_EXTERNAL_URL:http://localhost:8080}") String appBaseUrl) {

        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.javaMailSender = javaMailSender;
        this.appMail = appMail;
        this.appBaseUrl = appBaseUrl.stripTrailing().replaceAll("/$", "");
    }

    public void sendRegistrationEmail(String email, String username, String fullName, Locale locale,
                                      String token) {

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            mimeMessageHelper.setFrom(appMail);
            mimeMessageHelper.setReplyTo(appMail);
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject(getEmailActivationSubject(locale));
            mimeMessageHelper.setText(generateMessageContentActivation(locale, username, fullName, token), true);

            javaMailSender.send(mimeMessageHelper.getMimeMessage());

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendForgotPassEmail(String email, String username, String fullName,  Locale locale,
                                    String token) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            mimeMessageHelper.setFrom(appMail);
            mimeMessageHelper.setReplyTo(appMail);
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject(getEmailForgotPassSubject(locale));
            mimeMessageHelper.setText(generateMessageContentForgotPass(locale, username, fullName, token), true);

            javaMailSender.send(mimeMessageHelper.getMimeMessage());

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
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

    private String generateMessageContentForgotPass(Locale locale, String username, String fullName, String activationToken) {

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

