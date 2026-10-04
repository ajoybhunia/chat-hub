package com.chathub.modules.auth.dto;

/** Response body shared by signup and login: {@code {user, token}}. */
public record AuthResponse(UserResponse user, String token) {
}
