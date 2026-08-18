package com.quince.lawyeraiassistant.security.authentication;

import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.quince.lawyeraiassistant.security.identity.ApplicationUser;

public final class ApplicationUserDetails
        implements UserDetails {

    private final ApplicationUser user;

    public ApplicationUserDetails(
            ApplicationUser user) {

        this.user = Objects.requireNonNull(
                user,
                "user must not be null");
    }

    public ApplicationUser getApplicationUser() {

        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return user.roles()
                .stream()
                .map(
                        role -> new SimpleGrantedAuthority(
                                "ROLE_"
                                        + role.name()))
                .toList();
    }

    @Override
    public String getPassword() {

        return user.passwordHash();
    }

    @Override
    public String getUsername() {

        return user.username();
    }

    @Override
    public boolean isEnabled() {

        return user.enabled();
    }
}