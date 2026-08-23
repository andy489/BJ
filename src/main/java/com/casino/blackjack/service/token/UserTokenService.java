package com.casino.blackjack.service.token;

import com.casino.blackjack.model.entity.UserActivationTokenEntity;
import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.model.entity.UserForgotPassEntity;
import com.casino.blackjack.model.event.UserRegisteredEvent;
import com.casino.blackjack.model.event.UserForgotPasswordEvent;
import com.casino.blackjack.repo.UserActivationTokenRepository;
import com.casino.blackjack.repo.UserResetPassTokenRepository;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.service.mail.MailService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class UserTokenService {

    private static final Logger log = LoggerFactory.getLogger(UserTokenService.class);

    private static final String ACTIVATION_TOKEN_SYM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXY0123456789";

    private static final Integer ACTIVATION_TOKEN_LENGTH = 20;

    private final UserService userService;

    private final MailService mailService;

    private final UserActivationTokenRepository userActivationTokenRepository;
    private final UserResetPassTokenRepository userResetPassTokenRepository;

    public UserTokenService(UserService userService,
                            MailService mailService,
                            UserActivationTokenRepository userActivationTokenRepository,
                            UserResetPassTokenRepository userResetPassTokenRepository) {

        this.userService = userService;
        this.mailService = mailService;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.userResetPassTokenRepository = userResetPassTokenRepository;
    }

    @EventListener(UserRegisteredEvent.class)
    public void userRegistered(UserRegisteredEvent event) {
        String token = createActivationToken(event.getEmail());

        mailService.sendRegistrationEmail(event.getEmail(), event.getUsername(), event.getUserFullName(),
                event.getLocale(), token);
    }

    @EventListener(UserForgotPasswordEvent.class)
    public void userForgotPassword(UserForgotPasswordEvent event) {
        String token = createResetPassToken(event.getEmail());

        mailService.sendForgotPassEmail(event.getEmail(), event.getUsername(), event.getFullName(),
                event.getLocale(), token);
    }

    private String createActivationToken(String email) {

        String token = createToken();

        UserActivationTokenEntity userActivationTokenEntity = new UserActivationTokenEntity()
                .setToken(token)
                .setUser(userService.findByEmail(email).orElseThrow(
                        () -> new UsernameNotFoundException("User with email " + email + " not found")))
                .setCreatedAt(Instant.now());

        userActivationTokenRepository.save(userActivationTokenEntity);

        return token;
    }

    @Transactional
    public String createResetPassToken(String email) {

        String token = createToken();

        UserEntity userEntity = userService.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User with email " + email + " not found"));

        UserForgotPassEntity userForgotPassEntity = new UserForgotPassEntity()
                .setToken(token)
                .setUser(userEntity)
                .setCreatedAt(Instant.now());

        Optional<UserForgotPassEntity> byUserId = userResetPassTokenRepository.findByUserId(userEntity.getId());

        if(byUserId.isEmpty()){
            userResetPassTokenRepository.save(userForgotPassEntity);
        } else {
            UserForgotPassEntity userForgotPassEntityToModify = byUserId.get();

            userForgotPassEntityToModify
                    .setCreatedAt(userForgotPassEntity.getCreatedAt())
                    .setToken(userForgotPassEntity.getToken());

            userResetPassTokenRepository.save(userForgotPassEntityToModify);
        }

        return token;
    }

    private String createToken() {

        StringBuilder resetPassToken = new StringBuilder();
        Random random = new SecureRandom();

        for (int i = 0; i < ACTIVATION_TOKEN_LENGTH; i++) {
            int randInd = random.nextInt(ACTIVATION_TOKEN_SYM.length());

            resetPassToken.append(ACTIVATION_TOKEN_SYM.charAt(randInd));
        }

        return resetPassToken.toString();
    }

    @Transactional
    public void clearExpiredActivationTokens(LocalDateTime expiredTime) {

        Instant instant = expiredTime.toInstant(ZoneId.systemDefault().getRules().getOffset(expiredTime));

        log.debug("Clearing tokens before: {}", instant);

        userActivationTokenRepository.deleteByCreatedAtBefore(instant);
    }

    @Transactional
    public void clearExpiredForgotPassTokens(LocalDateTime expiredTime) {

        Instant instant = expiredTime.toInstant(ZoneId.systemDefault().getRules().getOffset(expiredTime));

        log.debug("Clearing tokens before: {}", instant);

        userResetPassTokenRepository.deleteByCreatedAtBefore(instant);
    }
}
