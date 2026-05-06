package com.casino.blackjack.controller;

import com.casino.blackjack.model.dto.RecaptchaResponseDTO;
import com.casino.blackjack.model.dto.ResetPasswordDTO;
import com.casino.blackjack.model.dto.UserRegistrationDTO;
import com.casino.blackjack.model.dto.UserResetPasswordSendInstructionsDTO;
import com.casino.blackjack.model.user.CustomUserDetails;
import com.casino.blackjack.service.recaptcha.RecaptchaService;
import com.casino.blackjack.service.auth.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    private final RecaptchaService recaptchaService;

    private final SecurityContextRepository securityContextRepository;

    private final LocaleResolver localeResolver;


    public AuthController(UserService userService,
                          RecaptchaService recaptchaService,
                          SecurityContextRepository securityContextRepository,
                          LocaleResolver localeResolver) {

        this.userService = userService;
        this.recaptchaService = recaptchaService;
        this.securityContextRepository = securityContextRepository;
        this.localeResolver = localeResolver;
    }

    @ModelAttribute(name = "userRegistrationDTO")
    public UserRegistrationDTO initUserRegisterDTO() {
        return new UserRegistrationDTO();
    }

    @GetMapping("/email")
    public ModelAndView getActivationMail() {
        return super.view("email/registration-activate");
    }

    @GetMapping("/register")
    public ModelAndView getRegister() {
        return super.view("auth/register");
    }

    @PostMapping("/register")
    public ModelAndView postRegister(
            @Valid @ModelAttribute(name = "userRegistrationDTO") UserRegistrationDTO userRegistrationDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "g-recaptcha-response") String recaptchaResponse,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boolean isBot = !recaptchaService.verify(recaptchaResponse)
                .map(RecaptchaResponseDTO::isSuccess)
                .orElse(false);

        if (isBot) {
            LOGGER.warn("reCAPTCHA protected your website from spam and abuse");
            return super.redirect("/");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("userRegistrationDTO", userRegistrationDTO);
            redirectAttributes.addFlashAttribute(BINDING_RESULT_PATH + "userRegistrationDTO",
                    bindingResult);

            return super.redirect("/auth/register");
        }

        userService.registerAndLogin(userRegistrationDTO, localeResolver.resolveLocale(request), successfulAuth -> {

            SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
            SecurityContext context = strategy.createEmptyContext();
            context.setAuthentication(successfulAuth);
            strategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        });

        redirectAttributes.addFlashAttribute("email", userRegistrationDTO.getEmail());
        return super.redirect("register-success");
    }

    @GetMapping("/register-success")
    public ModelAndView registerSuccess() {
        return super.view("auth/register-success");
    }

    @GetMapping("/login")
    public ModelAndView getLogin(@RequestParam(required = false) Boolean forgot) {

        if (forgot != null && forgot) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.addObject("forgot", true);

            return super.view("auth/login", modelAndView);
        }

        return super.view("auth/login");
    }

    @PostMapping("/login-error")
    public ModelAndView onFailedLogin(
            RedirectAttributes redirectAttributes,
            @ModelAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY) String username) {

        redirectAttributes.addFlashAttribute("bad_credentials_login", true);
        redirectAttributes.addFlashAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY,
                username);

        return super.redirect("/auth/login");
    }

    @GetMapping("/activation")
    public ModelAndView activateRegistration(@RequestParam(name = "token") String token,
                                             HttpServletRequest request,
                                             HttpServletResponse response,
                                             RedirectAttributes redirectAttributes) {

        return super.redirect(userService.loginAfterTokenActivate(token, successfulAuth -> {

            SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
            SecurityContext context = strategy.createEmptyContext();
            context.setAuthentication(successfulAuth);
            strategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        }, redirectAttributes));
    }

    @PostMapping("/reset-password")
    public ModelAndView sendResetPasswordInstructions(
            @Valid @ModelAttribute(name = "userResetPasswordSendInstructionsDTO")
            UserResetPasswordSendInstructionsDTO userResetPasswordSendInstructionsDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            @RequestParam(value = "g-recaptcha-response") String recaptchaResponse) {

        boolean isBot = !recaptchaService.verify(recaptchaResponse)
                .map(RecaptchaResponseDTO::isSuccess)
                .orElse(false);

        if (isBot) {
            LOGGER.warn("reCAPTCHA protected your website from spam and abuse");
            return super.redirect("/");
        }

        redirectAttributes.addFlashAttribute("email", userResetPasswordSendInstructionsDTO.getEmail());

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("userResetPasswordSendInstructionsDTO",
                    userResetPasswordSendInstructionsDTO);
            redirectAttributes.addFlashAttribute(BINDING_RESULT_PATH +
                            "userResetPasswordSendInstructionsDTO",
                    bindingResult);

            return super.redirect("/auth/reset?send=false");
        }

        userService.sendResetPasswordLink(userResetPasswordSendInstructionsDTO, localeResolver.resolveLocale(request),
                redirectAttributes);

        return super.redirect("/auth/reset?send=true");
    }

    @GetMapping("/reset")
    public ModelAndView getReset(@RequestParam Boolean send) {

        if (send != null && send) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.addObject("success", true);
            return super.view("auth/reset-pass-send", modelAndView);
        }

        return super.view("auth/reset-pass-send");
    }

    @ModelAttribute(name = "resetPasswordDTO")
    public ResetPasswordDTO initResetPasswordDTO() {
        return new ResetPasswordDTO();
    }

    @GetMapping("/reset_pass")
    public ModelAndView getChangePasswordWithToken(@RequestParam(required = false) String token) {

        return super.view("auth/reset-pass-form");
    }

    @PostMapping("/reset_pass")
    public ModelAndView postChangePassword(
            @Valid @ModelAttribute(name = "resetPasswordDTO") ResetPasswordDTO resetPasswordDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @RequestParam(required = false) String token,
            @RequestParam(value = "g-recaptcha-response") String recaptchaResponse) {

        boolean isBot = !recaptchaService.verify(recaptchaResponse)
                .map(RecaptchaResponseDTO::isSuccess)
                .orElse(false);

        if (isBot) {
            LOGGER.warn("reCAPTCHA protected your website from spam and abuse");
            return super.redirect("/");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("resetPasswordDTO", resetPasswordDTO);
            redirectAttributes.addFlashAttribute(BINDING_RESULT_PATH + "resetPasswordDTO",
                    bindingResult);

            return super.redirect("/auth/reset_pass?token=" + token);
        }

        return redirect(userService.changePassword(token, resetPasswordDTO.getPassword()));
    }

    @GetMapping("/pass")
    public ModelAndView getResetPassResponsePage(@RequestParam Boolean changed) {

        if (changed) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.addObject("changed", true);
            return super.view("auth/reset-pass-response", modelAndView);
        }

        return super.view("auth/reset-pass-response");
    }
}


