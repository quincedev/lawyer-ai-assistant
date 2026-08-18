package com.quince.lawyeraiassistant.security.tenant;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.identity.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextTest {

    @Test
    void shouldCreateValidTenantContext() {

        TenantContext context = new TenantContext(
                " tenant-a ",
                " user-001 ",
                " quince ",
                Set.of(
                        UserRole.LAWYER));

        assertEquals(
                "tenant-a",
                context.tenantId());

        assertEquals(
                "user-001",
                context.userId());

        assertEquals(
                "quince",
                context.username());

        assertTrue(
                context.hasRole(
                        UserRole.LAWYER));
    }

    @Test
    void shouldRejectBlankTenantId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TenantContext(
                        " ",
                        "user-001",
                        "quince",
                        Set.of(
                                UserRole.LAWYER)));
    }

    @Test
    void shouldRejectEmptyRoles() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TenantContext(
                        "tenant-a",
                        "user-001",
                        "quince",
                        Set.of()));
    }
}