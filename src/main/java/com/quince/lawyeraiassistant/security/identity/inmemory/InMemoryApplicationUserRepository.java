package com.quince.lawyeraiassistant.security.identity.inmemory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;

public final class InMemoryApplicationUserRepository
        implements ApplicationUserRepository {

    private final Map<String, ApplicationUser> usersByUsername;

    public InMemoryApplicationUserRepository(
            Collection<ApplicationUser> users) {

        Objects.requireNonNull(
                users,
                "users must not be null");

        Map<String, ApplicationUser> indexedUsers = new LinkedHashMap<>();

        for (ApplicationUser user : users) {

            Objects.requireNonNull(
                    user,
                    "user must not be null");

            ApplicationUser previous = indexedUsers.put(
                    user.username(),
                    user);

            if (previous != null) {

                throw new IllegalArgumentException(
                        "Duplicate username: "
                                + user.username());
            }
        }

        this.usersByUsername = Map.copyOf(
                indexedUsers);
    }

    @Override
    public Optional<ApplicationUser> findByUsername(
            String username) {

        if (username == null
                || username.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
                usersByUsername.get(
                        username.trim()));
    }
}