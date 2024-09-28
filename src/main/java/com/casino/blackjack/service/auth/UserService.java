package com.casino.blackjack.service.auth;

import com.casino.blackjack.model.dto.UserRegistrationDTO;
import com.casino.blackjack.model.dto.UserResetPasswordSendInstructionsDTO;
import com.casino.blackjack.model.entity.RoleEntity;
import com.casino.blackjack.model.entity.UserActivationTokenEntity;
import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.model.entity.UserForgotPassEntity;
import com.casino.blackjack.model.enumerated.UserRoleEnum;
import com.casino.blackjack.model.event.UserRegisteredEvent;
import com.casino.blackjack.model.event.UserForgotPasswordEvent;
import com.casino.blackjack.model.user.CustomUserDetails;
import com.casino.blackjack.repo.RoleRepository;
import com.casino.blackjack.repo.UserActivationTokenRepository;
import com.casino.blackjack.repo.UserRepository;
import com.casino.blackjack.repo.UserResetPassTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder encoder;

    private final UserDetailsService userDetailsService;

    private final AppUserDetailsService appUserDetailsService;

    private final Boolean autoLogin;

    private final ApplicationEventPublisher appEventPublisher;

    private final UserActivationTokenRepository userActivationTokenRepository;

    private final UserResetPassTokenRepository userResetPassTokenRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder encoder,
                       UserDetailsService userDetailsService,
                       @Value("${auth.register.auto-login}") Boolean autoLogin,
                       ApplicationEventPublisher appEventPublisher,
                       UserActivationTokenRepository userActivationTokenRepository,
                       UserResetPassTokenRepository userResetPassTokenRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.userDetailsService = userDetailsService;
        this.autoLogin = autoLogin;
        this.appUserDetailsService = new AppUserDetailsService(userRepository);
        this.appEventPublisher = appEventPublisher;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.userResetPassTokenRepository = userResetPassTokenRepository;
    }

    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void deleteActivationTokenByUserId(Long userId) {
        userActivationTokenRepository.deleteByUserId(userId);
    }

    public void deleteByUsername(String username) {
        userRepository.deleteByUsername(username);
    }

    public void deleteByEmail(String email) {
        userRepository.deleteByEmail(email);
    }

    public void registerAndLogin(UserRegistrationDTO userRegistrationDTO,
                                 Locale locale,
                                 Consumer<Authentication> successfulLoginProcessor) {
        // START: map birthDate
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(userRegistrationDTO.getBirthDate(), formatter);

        OffsetDateTime odt = OffsetDateTime.now(ZoneId.systemDefault());
        ZoneOffset zoneOffset = odt.getOffset();

        Date date = Date.from(localDate.atStartOfDay().toInstant(zoneOffset));
        // EO: map birthDate

        // START: map roles
        RoleEntity regularRole = roleRepository.findByRole(UserRoleEnum.REGULAR)
                .orElseThrow(() -> new RuntimeException("Failed to register regular user"));
        // EO: map roles

        UserEntity newUser = new UserEntity()
                .setUsername(userRegistrationDTO.getUsername())
                .setPassword(encoder.encode(userRegistrationDTO.getPassword()))
                .setEmail(userRegistrationDTO.getEmail())
                .setGender(userRegistrationDTO.getGender())
                .setBirthDate(date)
                .setRoles(Set.of(regularRole))
                .setFirstName(userRegistrationDTO.getFirstName())
                .setLastName(userRegistrationDTO.getLastName())
                .setIsActive(false)
                .setMyGame(null)
                .setMyWallet(null);

        newUser = userRepository.save(newUser);

        UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(getClass().getName(),
                userRegistrationDTO.getEmail(), userRegistrationDTO.getUsername(), userRegistrationDTO.getFullName(),
                locale);

        appEventPublisher.publishEvent(userRegisteredEvent);

        if (autoLogin) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(newUser.getUsername());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, // principal
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );

            successfulLoginProcessor.accept(authentication);
        }
    }

    public Authentication login(String username) {
        UserDetails userDetails = appUserDetailsService.loadUserByUsername(username);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        return auth;
    }

    @Transactional
    public String loginAfterTokenActivate(String activationToken,
                                          Consumer<Authentication> successfulLoginProcessor,
                                          RedirectAttributes redirectAttributes) {

        Optional<UserActivationTokenEntity> byActivationToken =
                userActivationTokenRepository.findByToken(activationToken);

        if (byActivationToken.isEmpty()) {
            return "/";
        }

        UserActivationTokenEntity userActivationTokenEntity = byActivationToken.get();
        Long userId = userActivationTokenEntity.getUser().getId();
        UserEntity referenceById = userRepository.getReferenceById(userId);
        referenceById.setIsActive(true);

        userActivationTokenRepository.deleteById(userActivationTokenEntity.getId());

        String username = referenceById.getUsername();

        redirectAttributes.addFlashAttribute("username", username);

        UserDetails userDetails = appUserDetailsService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );

        successfulLoginProcessor.accept(authentication);

        return "/";
    }

    @Transactional
    public void sendResetPasswordLink(UserResetPasswordSendInstructionsDTO userResetPasswordSendInstructionsDTO, Locale locale,
                                      RedirectAttributes redirectAttributes) {

        String email = userResetPasswordSendInstructionsDTO.getEmail();

        Optional<UserEntity> byEmail = userRepository.findByEmail(email);

        if (byEmail.isEmpty()) {
            throw new IllegalStateException("No such user");
        }

        UserEntity userEntity = byEmail.get();

        redirectAttributes.addFlashAttribute("username", userEntity.getUsername());

        UserForgotPasswordEvent userForgotPasswordEvent = new UserForgotPasswordEvent(getClass().getName(),
                userEntity.getEmail(), userEntity.getUsername(), userEntity.getFullName(), locale);

        appEventPublisher.publishEvent(userForgotPasswordEvent);
    }

    @Transactional
    public String changePassword(String token, String rawPass) {

        Optional<UserForgotPassEntity> byToken = userResetPassTokenRepository.findByToken(token);

        if (byToken.isEmpty()) {
            return "/auth/pass?changed=false";
        }

        UserForgotPassEntity userForgotPassEntity = byToken.get();

        UserEntity currentUser = userRepository.getReferenceById(userForgotPassEntity.getUser().getId());

        currentUser.setPassword(encoder.encode(rawPass));

        userResetPassTokenRepository.deleteByUserId(currentUser.getId());

        return "/auth/pass?changed=true";
    }

    public Boolean isUserLogged(CustomUserDetails principal) {
        return principal != null;
    }

    public Long getCurrentLoggedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

            return principal.getId();
        } else {
            return null;
        }
    }

    public UserEntity getCurrentLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

            return userRepository.getReferenceById(principal.getId());
        } else {
            return null;
        }
    }
}
