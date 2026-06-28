package com.mediflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class EnabledUserJwtValidatorTest {

@Mock
private UserRepository userRepository;

@Test
void enabledUserTokenIsAccepted() {
    User user = new User();
    user.setEmail("enabled@example.com");
    user.setEnabled(true);

    when(
        userRepository.findByEmail(
            "enabled@example.com"
        )
    )
        .thenReturn(Optional.of(user));

    EnabledUserJwtValidator validator =
        new EnabledUserJwtValidator(
            userRepository
        );

    OAuth2TokenValidatorResult result =
        validator.validate(
            createJwt("enabled@example.com")
        );

    assertThat(result.hasErrors()).isFalse();
}

@Test
void disabledUserTokenIsRejected() {
    User user = new User();
    user.setEmail("disabled@example.com");
    user.setEnabled(false);

    when(
        userRepository.findByEmail(
            "disabled@example.com"
        )
    )
        .thenReturn(Optional.of(user));

    EnabledUserJwtValidator validator =
        new EnabledUserJwtValidator(
            userRepository
        );

    OAuth2TokenValidatorResult result =
        validator.validate(
            createJwt("disabled@example.com")
        );

    assertThat(result.hasErrors()).isTrue();

    assertThat(result.getErrors())
        .extracting(error -> error.getErrorCode())
        .containsExactly("invalid_token");
}

@Test
void tokenWithoutSubjectIsRejected() {
    EnabledUserJwtValidator validator =
        new EnabledUserJwtValidator(
            userRepository
        );

    Jwt jwt = Jwt.withTokenValue("test-token")
        .header("alg", "HS256")
        .issuedAt(Instant.now())
        .expiresAt(
            Instant.now().plusSeconds(3600)
        )
        .build();

    OAuth2TokenValidatorResult result =
        validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();

    verifyNoInteractions(userRepository);
}

private Jwt createJwt(String subject) {
    Instant now = Instant.now();

    return Jwt.withTokenValue("test-token")
        .header("alg", "HS256")
        .subject(subject)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .build();
}

}