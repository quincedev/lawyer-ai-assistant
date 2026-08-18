package com.quince.lawyeraiassistant.security.authentication;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SecurityTest
class ApplicationUserDetailsServiceTest {

    @Test
    void shouldLoadExistingUser() {
        ApplicationUser user = user();
        ApplicationUserRepository repository = mock(ApplicationUserRepository.class);
        when(repository.findByUsername("quince")).thenReturn(Optional.of(user));

        ApplicationUserDetails details = (ApplicationUserDetails)
                new ApplicationUserDetailsService(repository)
                        .loadUserByUsername("quince");

        assertEquals(user, details.getApplicationUser());
        assertEquals("quince", details.getUsername());
        assertEquals("ROLE_LAWYER", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldRejectUnknownUser() {
        ApplicationUserRepository repository = mock(ApplicationUserRepository.class);
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

        ApplicationUserDetailsService service = new ApplicationUserDetailsService(repository);

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown"));
    }

    private static ApplicationUser user() {
        return new ApplicationUser(
                "user-001",
                "tenant-a",
                "quince",
                "{noop}password123",
                Set.of(UserRole.LAWYER),
                true);
    }
}
