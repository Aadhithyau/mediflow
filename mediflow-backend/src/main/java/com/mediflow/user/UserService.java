package com.mediflow.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.user.dto.CurrentUserResponse;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
            .filter(User::isEnabled)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User account is unavailable"
            ));

        return new CurrentUserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name(),
            user.isEnabled()
        );
    }
}