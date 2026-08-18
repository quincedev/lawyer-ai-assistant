package com.quince.lawyeraiassistant.security.authentication;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.authentication.api.LoginResponse;
import com.quince.lawyeraiassistant.security.authentication.config.JwtSecurityProperties;
import com.quince.lawyeraiassistant.security.authentication.jwt.JwtTokenService;
import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SecurityTest
class DefaultAuthenticationServiceTest {

    @Test
    void shouldAuthenticateCredentialsAndReturnBearerToken() {
        ApplicationUser user = new ApplicationUser(
                "user-001",
                "tenant-a",
                "quince",
                "hash",
                Set.of(UserRole.LAWYER),
                true);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtTokenService tokenService = mock(JwtTokenService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new ApplicationUserDetails(user));
        when(manager.authenticate(org.mockito.ArgumentMatchers.any())).thenReturn(authentication);
        when(tokenService.createAccessToken(user)).thenReturn("signed-jwt");
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setAccessTokenTtl(Duration.ofHours(1));

        LoginResponse response = new DefaultAuthenticationService(
                manager,
                tokenService,
                properties).login("quince", "password123");

        ArgumentCaptor<Authentication> request = ArgumentCaptor.forClass(Authentication.class);
        verify(manager).authenticate(request.capture());
        assertEquals("quince", request.getValue().getPrincipal());
        assertEquals("password123", request.getValue().getCredentials());
        assertFalse(request.getValue().isAuthenticated());
        assertEquals("signed-jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
    }
}
