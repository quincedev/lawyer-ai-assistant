package com.quince.lawyeraiassistant.security.identity.persistence.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.UserRole;

class MyBatisApplicationUserRepositoryTest {

    private ApplicationUserMapper userMapper;

    private MyBatisApplicationUserRepository repository;

    @BeforeEach
    void setUp() {

        userMapper = mock(
                ApplicationUserMapper.class);

        repository = new MyBatisApplicationUserRepository(
                userMapper);
    }

    @Test
    void shouldReturnEmptyWithoutCallingMapperForBlankUsername() {

        assertTrue(
                repository.findByUsername(
                        "   ")
                        .isEmpty());

        verifyNoInteractions(
                userMapper);
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {

        when(userMapper.findRowsByUsername(
                "unknown"))
                .thenReturn(
                        List.of());

        Optional<ApplicationUser> result = repository.findByUsername(
                " unknown ");

        assertTrue(
                result.isEmpty());

        verify(userMapper).findRowsByUsername(
                "unknown");
    }

    @Test
    void shouldMapSingleRoleUser() {

        when(userMapper.findRowsByUsername(
                "quince"))
                .thenReturn(
                        List.of(
                                row(
                                        "LAWYER")));

        ApplicationUser user = repository.findByUsername(
                "quince")
                .orElseThrow();

        assertEquals(
                "user-001",
                user.id());

        assertEquals(
                "tenant-a",
                user.tenantId());

        assertEquals(
                "quince",
                user.username());

        assertEquals(
                "{bcrypt}password-hash",
                user.passwordHash());

        assertEquals(
                Set.of(
                        UserRole.LAWYER),
                user.roles());

        assertTrue(
                user.enabled());
    }

    @Test
    void shouldAggregateMultipleRoles() {

        when(userMapper.findRowsByUsername(
                "quince"))
                .thenReturn(
                        List.of(
                                row(
                                        "LAWYER"),
                                row(
                                        "ADMIN")));

        ApplicationUser user = repository.findByUsername(
                "quince")
                .orElseThrow();

        assertEquals(
                Set.of(
                        UserRole.LAWYER,
                        UserRole.ADMIN),
                user.roles());
    }

    @Test
    void shouldFailClosedWhenRoleIsMissing() {

        when(userMapper.findRowsByUsername(
                "quince"))
                .thenReturn(
                        List.of(
                                row(
                                        null)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> repository.findByUsername(
                        "quince"));

        assertTrue(
                exception.getMessage()
                        .contains(
                                "role must not be blank"));
    }

    @Test
    void shouldFailClosedForUnsupportedRole() {

        when(userMapper.findRowsByUsername(
                "quince"))
                .thenReturn(
                        List.of(
                                row(
                                        "SUPER_USER")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> repository.findByUsername(
                        "quince"));

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Unsupported application user role"));
    }

    @Test
    void shouldRejectInconsistentRows() {

        ApplicationUserRow first = row(
                "LAWYER");

        ApplicationUserRow inconsistent = new ApplicationUserRow(
                "user-999",
                "tenant-b",
                "quince",
                "{bcrypt}another-hash",
                false,
                "ADMIN");

        when(userMapper.findRowsByUsername(
                "quince"))
                .thenReturn(
                        List.of(
                                first,
                                inconsistent));

        assertThrows(
                IllegalStateException.class,
                () -> repository.findByUsername(
                        "quince"));
    }

    @Test
    void shouldPreserveDisabledState() {

        ApplicationUserRow disabled = new ApplicationUserRow(
                "user-001",
                "tenant-a",
                "quince",
                "{bcrypt}password-hash",
                false,
                "LAWYER");

        when(userMapper.findRowsByUsername(
                "quince"))
                .thenReturn(
                        List.of(
                                disabled));

        ApplicationUser user = repository.findByUsername(
                "quince")
                .orElseThrow();

        assertFalse(
                user.enabled());
    }

    @Test
    void shouldRejectNullMapperDependency() {

        assertEquals(
                "userMapper must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> new MyBatisApplicationUserRepository(
                                null))
                        .getMessage());
    }

    private ApplicationUserRow row(
            String role) {

        return new ApplicationUserRow(
                "user-001",
                "tenant-a",
                "quince",
                "{bcrypt}password-hash",
                true,
                role);
    }
}