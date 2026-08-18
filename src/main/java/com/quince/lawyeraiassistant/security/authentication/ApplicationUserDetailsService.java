package com.quince.lawyeraiassistant.security.authentication;

import java.util.Objects;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;

public final class ApplicationUserDetailsService
        implements UserDetailsService {

    private final ApplicationUserRepository userRepository;

    public ApplicationUserDetailsService(
            ApplicationUserRepository userRepository) {

        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository must not be null");
    }

    @Override
    public UserDetails loadUserByUsername(
            String username)
            throws UsernameNotFoundException {

        return userRepository
                .findByUsername(
                        username)
                .map(
                        ApplicationUserDetails::new)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "User not found"));
    }
}