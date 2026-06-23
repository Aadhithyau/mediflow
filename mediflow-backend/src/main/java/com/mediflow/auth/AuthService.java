package com.mediflow.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.auth.dto.RegisterRequest;
import com.mediflow.auth.dto.RegisterResponse;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        user.setPasswordHash(passwordEncoder.encode(request.password()));
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
}