package com.quince.lawyeraiassistant.security.authentication.config;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.identity.inmemory.InMemoryApplicationUserRepository;

@Configuration(proxyBeanMethods = false)
@Profile("!prod")
public class DevelopmentIdentityConfiguration {

    @Bean
    ApplicationUserRepository applicationUserRepository(
            PasswordEncoder passwordEncoder) {

        ApplicationUser lawyer = new ApplicationUser(
                "user-001",
                "tenant-a",
                "quince",
                passwordEncoder.encode(
                        "password123"),
                Set.of(
                        UserRole.LAWYER),
                true);

        ApplicationUser director = new ApplicationUser(
                "user-002",
                "tenant-a",
                "director",
                passwordEncoder.encode(
                        "password123"),
                Set.of(
                        UserRole.DIRECTOR),
                true);

        ApplicationUser tenantBUser = new ApplicationUser(
                "user-003",
                "tenant-b",
                "alice",
                passwordEncoder.encode(
                        "password123"),
                Set.of(
                        UserRole.LAWYER),
                true);

        return new InMemoryApplicationUserRepository(
                List.of(
                        lawyer,
                        director,
                        tenantBUser));
    }
}