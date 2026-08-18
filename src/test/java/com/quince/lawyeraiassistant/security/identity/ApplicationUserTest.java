package com.quince.lawyeraiassistant.security.identity;

import com.quince.lawyeraiassistant.security.SecurityTest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SecurityTest
class ApplicationUserTest {

    @Test
    void shouldCreateValidUser() {
        ApplicationUser user = new ApplicationUser(
                " user-1 ",
                " tenant-1 ",
                " lawyer ",
                " hash ",
                Set.of(UserRole.LAWYER),
                true);

        assertEquals("user-1", user.id());
        assertEquals("tenant-1", user.tenantId());
        assertEquals("lawyer", user.username());
        assertEquals("hash", user.passwordHash());
        assertEquals(Set.of(UserRole.LAWYER), user.roles());
        assertEquals(true, user.enabled());
    }

    @Test
    void shouldRejectBlankTenantId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createUser("   ", "hash", Set.of(UserRole.LAWYER)));
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createUser("tenant-1", "   ", Set.of(UserRole.LAWYER)));
    }

    @Test
    void shouldRejectEmptyRoles() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createUser("tenant-1", "hash", Set.of()));
    }

    private static ApplicationUser createUser(
            String tenantId,
            String passwordHash,
            Set<UserRole> roles) {
        return new ApplicationUser(
                "user-1",
                tenantId,
                "lawyer",
                passwordHash,
                roles,
                true);
    }
}
