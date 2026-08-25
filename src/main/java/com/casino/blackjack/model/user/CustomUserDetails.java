package com.casino.blackjack.model.user;

import com.casino.blackjack.model.enumerated.GenderEnum;
import com.casino.blackjack.util.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;

@Getter
@Setter
@Accessors(chain = true)
public class CustomUserDetails implements UserDetails {

    private Long id;

    private String username;

    private String password;

    private GenderEnum gender;

    private String email;

    private Long birthDate;

    private String firstName;

    private String lastName;

    private Collection<? extends GrantedAuthority> authorities;

    public Integer getAge() {
        if (birthDate == null) {
            throw new IllegalStateException("birthDate is not set");
        }

        return DateUtil.calcYearsBetween(birthDate, LocalDate.now());
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
