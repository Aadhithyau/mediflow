package com.mediflow.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.auth.dto.LoginRequest;
import com.mediflow.auth.dto.LoginResponse;
import com.mediflow.auth.dto.RegisterRequest;
import com.mediflow.auth.dto.RegisterResponse;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already registered"
            );
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
            passwordEncoder.encode(request.password())
        );
        user.setRole(Role.PATIENT);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
            savedUser.getId(),
            savedUser.getFullName(),
            savedUser.getEmail(),
            savedUser.getRole().name(),
            "Patient registered successfully"
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String normalizedEmail = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            ));

        boolean passwordMatches = passwordEncoder.matches(
            request.password(),
            user.getPasswordHash()
        );

        if (!passwordMatches || !user.isEnabled()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            );
        }

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
            accessToken,
            "Bearer",
            jwtService.getExpirationSeconds(),
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name()
        );
    }
}