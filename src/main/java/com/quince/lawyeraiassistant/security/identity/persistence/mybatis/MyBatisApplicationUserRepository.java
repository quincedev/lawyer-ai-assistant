package com.quince.lawyeraiassistant.security.identity.persistence.mybatis;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.UserRole;

/**
 * MyBatis-backed ApplicationUserRepository adapter.
 *
 * <p>
 * Converts persistence rows into the identity domain model.
 * Spring bean/profile registration is handled separately.
 * </p>
 */
public final class MyBatisApplicationUserRepository
        implements ApplicationUserRepository {

    private final ApplicationUserMapper userMapper;

    public MyBatisApplicationUserRepository(
            ApplicationUserMapper userMapper) {

        this.userMapper = Objects.requireNonNull(
                userMapper,
                "userMapper must not be null");
    }

    @Override
    public Optional<ApplicationUser> findByUsername(
            String username) {

        if (username == null
                || username.isBlank()) {

            return Optional.empty();
        }

        String normalizedUsername = username.trim();

        List<ApplicationUserRow> rows = Objects.requireNonNull(
                userMapper.findRowsByUsername(
                        normalizedUsername),
                "ApplicationUserMapper result must not be null");

        if (rows.isEmpty()) {

            return Optional.empty();
        }

        ApplicationUserRow firstRow = Objects.requireNonNull(
                rows.getFirst(),
                "ApplicationUser row must not be null");

        EnumSet<UserRole> roles = EnumSet.noneOf(
                UserRole.class);

        for (ApplicationUserRow row : rows) {

            Objects.requireNonNull(
                    row,
                    "ApplicationUser row must not be null");

            validateSameUser(
                    firstRow,
                    row);

            roles.add(
                    parseRole(
                            row.role()));
        }

        if (roles.isEmpty()) {

            throw new IllegalStateException(
                    "Application user must have at least one role: "
                            + firstRow.id());
        }

        return Optional.of(
                new ApplicationUser(
                        firstRow.id(),
                        firstRow.tenantId(),
                        firstRow.username(),
                        firstRow.passwordHash(),
                        Set.copyOf(
                                roles),
                        firstRow.enabled()));
    }

    private void validateSameUser(
            ApplicationUserRow expected,
            ApplicationUserRow actual) {

        boolean sameUser = Objects.equals(
                expected.id(),
                actual.id())
                && Objects.equals(
                        expected.tenantId(),
                        actual.tenantId())
                && Objects.equals(
                        expected.username(),
                        actual.username())
                && Objects.equals(
                        expected.passwordHash(),
                        actual.passwordHash())
                && expected.enabled() == actual.enabled();

        if (!sameUser) {

            throw new IllegalStateException(
                    "Inconsistent application user rows for username: "
                            + expected.username());
        }
    }

    private UserRole parseRole(
            String role) {

        if (role == null
                || role.isBlank()) {

            throw new IllegalStateException(
                    "Application user role must not be blank");
        }

        try {

            return UserRole.valueOf(
                    role.trim());

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Unsupported application user role: "
                            + role,
                    exception);
        }
    }
}