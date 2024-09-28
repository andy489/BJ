package com.casino.blackjack.config.security;

import com.casino.blackjack.repo.UserRepository;
import com.casino.blackjack.service.auth.AppUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity // for @PreAuthorize to work
public class SecurityConfig {

    private final String rememberMeKey;

    public SecurityConfig(@Value("${auth.login.remember-me-key}") String rememberMeKey) {
        this.rememberMeKey = rememberMeKey;
    }

    @Bean
    public PasswordEncoder encode() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http // .csrf(AbstractHttpConfigurer::disable)
                // defines which pages will be authorized
                .authorizeHttpRequests(auth -> {
                    auth
                            // allow access to all static locations defined in StaticResourceLocation enum class
                            // (images, css, js, webjars, etc.)
                            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                            // the URLs below are available for all users - logged in and anonymous
                            .requestMatchers(
                                    "/",
                                    "/index",
                                    "/error/**",
                                    "/login-error",
                                    "/rules",
                                    "/test/**")
                            .permitAll()
                            .requestMatchers(
                                    "/credit-card/**",
                                    "/play/**"
                            ).authenticated()
                            .requestMatchers(HttpMethod.POST, "/auth/**")
                            .anonymous()
                            .requestMatchers(HttpMethod.GET, "/auth/**")
                            .permitAll()
                            .anyRequest()
                            .permitAll()
                    ;
                })
                .formLogin(form -> {
                    form
                            .loginPage("/auth/login")
                            .loginProcessingUrl("/auth/login")
                            .failureForwardUrl("/auth/login-error")
                            // where to go after login (use true arg if we want to go there, otherwise go to prev page)
                            .defaultSuccessUrl("/" /*,true*/) // arg alwaysUse: true
                            // the names of the "username" and "password" input fields in the custom login form
                            .usernameParameter(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY)
                            .passwordParameter(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY)
                            .permitAll();
                })
                .logout(logout -> {
                    logout
                            // the URL where we should POST in order to perform the logout
                            .logoutUrl("/auth/logout")
                            // where to go when logged out
                            .logoutSuccessUrl("/")
                            .clearAuthentication(true)
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll();
                })
                .securityContext(context -> {
                    context.securityContextRepository(securityContextRepository());
                })
                .rememberMe(rememberMeConfigurer -> {
                    rememberMeConfigurer
                            .key(rememberMeKey)
                            .tokenValiditySeconds(3600) // an hour
                            .rememberMeParameter("remember-me-parameter")
                            .rememberMeCookieName("remember-me-cookie");
                    // https://docs.spring.io/spring-security/reference/servlet/authentication/rememberme.html
                    // https://www.base64decode.org/
                })
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return new AppUserDetailsService(userRepository);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }
}
