package com.mediflow.user.dto;

public record CurrentUserResponse(
    Long id,
    String fullName,
    String email,
    String role,
    boolean enabled
) {
}