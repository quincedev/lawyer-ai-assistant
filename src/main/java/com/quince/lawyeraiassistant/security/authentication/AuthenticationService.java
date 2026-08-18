package com.quince.lawyeraiassistant.security.authentication;

import com.quince.lawyeraiassistant.security.authentication.api.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(
            String username,
            String password);
}