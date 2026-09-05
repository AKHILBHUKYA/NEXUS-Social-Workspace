package com.akhil.social.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            String displayName
    ) {}
    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password
    ) {}
    public record AuthResponse(String token, Long userId, String username, String displayName, String role) {}
    public record UserResponse(Long id, String username, String email, String displayName, String avatarUrl, String bio, String role) {}
}
