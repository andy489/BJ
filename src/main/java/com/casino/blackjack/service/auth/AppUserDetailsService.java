package com.casino.blackjack.service.auth;

import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.model.user.CustomUserDetails;
import com.casino.blackjack.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

// NOTE: This is not annotated as @Service, because we will return it as a bean.
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> userEntityOpt = userRepository.findByUsername(username);

        if (userEntityOpt.isEmpty()) {
            throw new UsernameNotFoundException("User with username " + username + " not found");
        }

        UserEntity userEntity = userEntityOpt.get();

        if (!userEntity.getIsActive()) {
            throw new UsernameNotFoundException("User with username " + username + " not found");
        }

        List<SimpleGrantedAuthority> simpleGrantedAuthorities =
                userEntity.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getRole().name())).toList();

        return new CustomUserDetails()
                .setId(userEntity.getId())
                .setUsername(userEntity.getUsername())
                .setPassword(userEntity.getPassword())
                .setGender(userEntity.getGender())
                .setEmail(userEntity.getEmail())
                .setBirthDate(userEntity.getBirthDate())
                .setFirstName(userEntity.getFirstName())
                .setLastName(userEntity.getLastName())
                .setAuthorities(simpleGrantedAuthorities);
    }
}
