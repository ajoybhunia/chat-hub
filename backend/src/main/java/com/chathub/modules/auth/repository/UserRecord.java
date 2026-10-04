package com.chathub.modules.auth.repository;

/**
 * Row-level user record as stored in the database — includes the password hash
 * (needed for login verification) and must never leave the service layer.
 */
public record UserRecord(int id, String username, String email, String passwordHash) {
}
