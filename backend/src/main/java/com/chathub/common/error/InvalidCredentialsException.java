package com.chathub.common.error;

/** Thrown when login credentials do not match any user. */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException(String message) {
		super(message);
	}
}
