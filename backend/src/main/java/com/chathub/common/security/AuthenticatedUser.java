package com.chathub.common.security;

/**
 * Verified identity extracted from a JWT. Attached to the current HTTP request
 * under {@link #REQUEST_ATTRIBUTE} by {@link JwtAuthFilter}.
 */
public record AuthenticatedUser(int id, String email) {

	public static final String REQUEST_ATTRIBUTE = "authenticatedUser";
}
