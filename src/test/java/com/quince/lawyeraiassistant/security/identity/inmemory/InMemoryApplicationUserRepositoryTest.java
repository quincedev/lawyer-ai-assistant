package com.quince.lawyeraiassistant.security.identity.inmemory;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class InMemoryApplicationUserRepositoryTest {

    @Test
    void shouldFindExistingUser() {
        ApplicationUser user = user("user-1", "lawyer");
        InMemoryApplicationUserRepository repository =
                new InMemoryApplicationUserRepository(List.of(user));

        assertEquals(user, repository.findByUsername(" lawyer ").orElseThrow());
    }

    @Test
    void shouldReturnEmptyForUnknownUser() {
        InMemoryApplicationUserRepository repository =
                new InMemoryApplicationUserRepository(List.of());

        assertTrue(repository.findByUsername("unknown").isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankUsername() {
        InMemoryApplicationUserRepository repository =
                new InMemoryApplicationUserRepository(List.of());

        assertTrue(repository.findByUsername("   ").isEmpty());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        ApplicationUser first = user("user-1", "lawyer");
        ApplicationUser duplicate = user("user-2", "lawyer");

        assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryApplicationUserRepository(
                        List.of(first, duplicate)));
    }

    private static ApplicationUser user(String id, String username) {
        return new ApplicationUser(
                id,
                "tenant-1",
                username,
                "hash",
                Set.of(UserRole.LAWYER),
                true);
    }
}
