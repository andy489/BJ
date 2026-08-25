package com.casino.blackjack.config.mail;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@ConfigurationProperties(prefix = "mail")
@Getter
@Setter
@Accessors(chain = true)
public class MailConfig {

    private String host;

    private Integer port;

    private String username;

    private String password;

    private String smtpAuth;

    private String transportProtocol;

    private String starttlsEnable;

    private String sslEnable;

    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

        javaMailSender.setHost(host);
        javaMailSender.setPort(port);

        javaMailSender.setUsername(username);
        javaMailSender.setPassword(password);

        javaMailSender.setJavaMailProperties(mailProperties());
        javaMailSender.setDefaultEncoding("UTF-8");

        return javaMailSender;
    }

    private Properties mailProperties() {

        Properties properties = new Properties();

        properties.setProperty("mail.smtp.auth", smtpAuth);
        properties.setProperty("mail.transport.protocol", transportProtocol);
        properties.setProperty("mail.smtp.connectiontimeout", "10000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");
        if (starttlsEnable != null) {
            properties.setProperty("mail.smtp.starttls.enable", starttlsEnable);
        }
        if (sslEnable != null) {
            properties.setProperty("mail.smtp.ssl.enable", sslEnable);
        }

        return properties;
    }
}
