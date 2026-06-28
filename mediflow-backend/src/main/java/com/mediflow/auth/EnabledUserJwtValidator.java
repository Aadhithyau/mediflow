package com.mediflow.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.mediflow.user.UserRepository;

@Component
public class EnabledUserJwtValidator
implements OAuth2TokenValidator<Jwt> {

private static final OAuth2Error INVALID_USER =
    new OAuth2Error(
        "invalid_token",
        "User account is disabled or unavailable",
        null
    );

private final UserRepository userRepository;

public EnabledUserJwtValidator(
    UserRepository userRepository
) {
    this.userRepository = userRepository;
}

@Override
public OAuth2TokenValidatorResult validate(Jwt jwt) {

    String email = jwt.getSubject();

    if (email == null || email.isBlank()) {
        return OAuth2TokenValidatorResult.failure(
            INVALID_USER
        );
    }

    boolean enabledUserExists = userRepository
        .findByEmail(email)
        .filter(user -> user.isEnabled())
        .isPresent();

    if (!enabledUserExists) {
        return OAuth2TokenValidatorResult.failure(
            INVALID_USER
        );
    }

    return OAuth2TokenValidatorResult.success();
}

}