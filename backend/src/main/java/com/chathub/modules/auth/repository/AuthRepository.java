package com.chathub.modules.auth.repository;

import java.util.Optional;

/** Persistence contract for the {@code users} table. */
public interface AuthRepository {

	Optional<UserRecord> findByEmail(String email);

	Optional<UserRecord> findByUsername(String username);

	UserRecord createUser(String username, String email, String passwordHash);
}
