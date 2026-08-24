package com.quince.lawyeraiassistant.security.identity.persistence.mybatis;

/**
 * MyBatis database row model.
 *
 * <p>
 * A user with multiple roles produces multiple rows.
 * This persistence model must not leak into the authentication layer.
 * </p>
 */
public record ApplicationUserRow(

        String id,

        String tenantId,

        String username,

        String passwordHash,

        boolean enabled,

        String role

) {
}