package com.chathub.common.security;

/**
 * Thrown when a JWT cannot be verified (malformed, expired, wrong signature,
 * missing claims). Callers translate it to a 401.
 */
public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message) {
		super(message);
	}

	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
