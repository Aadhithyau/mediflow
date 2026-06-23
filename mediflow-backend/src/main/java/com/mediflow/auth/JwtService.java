package com.mediflow.auth;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.mediflow.user.User;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtService(
        JwtEncoder jwtEncoder,
        @Value("${app.jwt.issuer}") String issuer,
        @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

        return jwtEncoder
            .encode(JwtEncoderParameters.from(header, claims))
            .getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}