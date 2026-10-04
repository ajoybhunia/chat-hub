package com.chathub.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /auth/signup}. Validation (issue #7) is stricter
 * than the Deno backend, which performed none.
 */
public record SignupRequest(
		@NotBlank @Size(max = 32) String username,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
