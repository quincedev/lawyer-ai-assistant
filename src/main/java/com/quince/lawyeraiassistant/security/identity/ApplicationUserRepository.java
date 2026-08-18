package com.quince.lawyeraiassistant.security.identity;

import java.util.Optional;

public interface ApplicationUserRepository {

    Optional<ApplicationUser> findByUsername(
            String username);
}