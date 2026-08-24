package com.quince.lawyeraiassistant.security.identity.persistence.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.UserRole;

@MybatisTest
@Testcontainers
@ActiveProfiles("persistence-test")
@Import(MyBatisApplicationUserRepository.class)
class MyBatisApplicationUserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer(
            "mysql:8.0")
            .withDatabaseName(
                    "lawyer_ai_test")
            .withUsername(
                    "lawyer_ai_test")
            .withPassword(
                    "test-password");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MyBatisApplicationUserRepository repository;

    @BeforeEach
    void setUpDatabase() {

        jdbcTemplate.update(
                "DELETE FROM application_user_role");

        jdbcTemplate.update(
                "DELETE FROM application_user");

        jdbcTemplate.update(
                """
                        INSERT INTO application_user
                        (
                            id,
                            tenant_id,
                            username,
                            password_hash,
                            enabled
                        )
                        VALUES
                        (
                            ?,
                            ?,
                            ?,
                            ?,
                            ?
                        )
                        """,
                "user-001",
                "tenant-a",
                "quince",
                "{bcrypt}test-password-hash",
                true);

        jdbcTemplate.update(
                """
                        INSERT INTO application_user_role
                        (
                            user_id,
                            role
                        )
                        VALUES
                        (
                            ?,
                            ?
                        )
                        """,
                "user-001",
                "LAWYER");
    }

    @Test
    void shouldLoadUserThroughRealMyBatisMapper() {

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
                "{bcrypt}test-password-hash",
                user.passwordHash());

        assertEquals(
                Set.of(
                        UserRole.LAWYER),
                user.roles());

        assertTrue(
                user.enabled());
    }

    @Test
    void shouldAggregateMultipleRolesFromJoinedRows() {

        jdbcTemplate.update(
                """
                        INSERT INTO application_user_role
                        (
                            user_id,
                            role
                        )
                        VALUES
                        (
                            ?,
                            ?
                        )
                        """,
                "user-001",
                "ADMIN");

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
    void shouldReturnEmptyForUnknownUsername() {

        assertTrue(
                repository.findByUsername(
                        "unknown")
                        .isEmpty());
    }

    @Test
    void shouldTrimUsernameBeforeQuery() {

        ApplicationUser user = repository.findByUsername(
                "  quince  ")
                .orElseThrow();

        assertEquals(
                "quince",
                user.username());
    }

    @Test
    void shouldPreserveDisabledState() {

        jdbcTemplate.update(
                """
                        UPDATE application_user
                        SET enabled = FALSE
                        WHERE id = ?
                        """,
                "user-001");

        ApplicationUser user = repository.findByUsername(
                "quince")
                .orElseThrow();

        assertFalse(
                user.enabled());
    }

    @Test
    void usernameLookupShouldRemainCaseSensitive() {

        assertTrue(
                repository.findByUsername(
                        "Quince")
                        .isEmpty());
    }
}