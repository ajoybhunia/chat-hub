package com.chathub.modules.auth.dto;

/**
 * Public user representation returned by the API. Deliberately excludes the
 * password hash — the Deno backend leaked it via {@code RETURNING *}.
 */
public record UserResponse(int id, String username, String email) {
}
