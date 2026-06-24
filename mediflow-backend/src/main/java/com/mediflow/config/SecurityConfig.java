package com.mediflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/api/auth/**",
                    "/error"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/doctors/*/availability-slots"
                )
                .hasRole("PATIENT")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/appointments"
                )
                .hasRole("PATIENT")

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/appointments"
                )
                .hasRole("PATIENT")

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/appointments/*/cancel"
                )
                .hasRole("PATIENT")

                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                .requestMatchers("/api/doctor/**")
                    .hasRole("DOCTOR")

                .requestMatchers("/api/users/me")
                    .hasAnyRole(
                        "PATIENT",
                        "DOCTOR",
                        "ADMIN"
                    )

                .anyRequest()
                    .authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(
                        jwtAuthenticationConverter
                    )
                )
            );

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
            new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    }
}