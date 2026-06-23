package com.mediflow.auth.dto;

public record RegisterResponse(
    Long id,
    String fullName,
    String email,
    String role,
    String message
) {
}