package com.quince.lawyeraiassistant.security.identity;

import com.quince.lawyeraiassistant.security.SecurityTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class AuthenticatedIdentityTest {

    @Test
    void shouldCreateValidIdentity() {
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                " user-1 ",
                " lawyer ",
                " tenant-1 ",
                Set.of(UserRole.LAWYER));

        assertEquals("user-1", identity.userId());
        assertEquals("lawyer", identity.username());
        assertEquals("tenant-1", identity.tenantId());
        assertEquals(Set.of(UserRole.LAWYER), identity.roles());
    }

    @Test
    void shouldRejectBlankUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticatedIdentity(
                        "   ",
                        "lawyer",
                        "tenant-1",
                        Set.of(UserRole.LAWYER)));
    }

    @Test
    void shouldRejectBlankTenantId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticatedIdentity(
                        "user-1",
                        "lawyer",
                        "   ",
                        Set.of(UserRole.LAWYER)));
    }

    @Test
    void shouldRejectEmptyRoles() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticatedIdentity(
                        "user-1",
                        "lawyer",
                        "tenant-1",
                        Set.of()));
    }

    @Test
    void shouldKeepRolesImmutable() {
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.LAWYER);
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                "user-1",
                "lawyer",
                "tenant-1",
                roles);

        roles.add(UserRole.ADMIN);

        assertEquals(Set.of(UserRole.LAWYER), identity.roles());
        assertThrows(
                UnsupportedOperationException.class,
                () -> identity.roles().add(UserRole.ADMIN));
    }

    @Test
    void shouldReportWhetherIdentityHasRole() {
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                "user-1",
                "lawyer",
                "tenant-1",
                Set.of(UserRole.LAWYER));

        assertTrue(identity.hasRole(UserRole.LAWYER));
        assertFalse(identity.hasRole(UserRole.ADMIN));
    }
}
