package com.chathub.common.error;

/** Thrown when a signup conflicts with an existing email or username. */
public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}
}
