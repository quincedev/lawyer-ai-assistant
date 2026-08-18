package com.quince.lawyeraiassistant.security.authentication;

import java.util.Objects;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.security.authentication.api.LoginResponse;
import com.quince.lawyeraiassistant.security.authentication.config.JwtSecurityProperties;
import com.quince.lawyeraiassistant.security.authentication.jwt.JwtTokenService;
import com.quince.lawyeraiassistant.security.identity.ApplicationUser;

@Service
public final class DefaultAuthenticationService
        implements AuthenticationService {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenService jwtTokenService;

    private final JwtSecurityProperties jwtProperties;

    public DefaultAuthenticationService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            JwtSecurityProperties jwtProperties) {

        this.authenticationManager = Objects.requireNonNull(
                authenticationManager,
                "authenticationManager must not be null");

        this.jwtTokenService = Objects.requireNonNull(
                jwtTokenService,
                "jwtTokenService must not be null");

        this.jwtProperties = Objects.requireNonNull(
                jwtProperties,
                "jwtProperties must not be null");
    }

    @Override
    public LoginResponse login(
            String username,
            String password) {

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                username,
                                password));

        ApplicationUserDetails principal = (ApplicationUserDetails) authentication.getPrincipal();

        ApplicationUser user = principal.getApplicationUser();

        String accessToken = jwtTokenService.createAccessToken(
                user);

        return LoginResponse.bearer(
                accessToken,
                jwtProperties
                        .getAccessTokenTtl()
                        .toSeconds());
    }
}