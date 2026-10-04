package com.chathub.modules.auth.service;

import com.chathub.common.error.DuplicateResourceException;
import com.chathub.common.error.InvalidCredentialsException;
import com.chathub.common.logging.LogDetails;
import com.chathub.common.security.JwtService;
import com.chathub.modules.auth.dto.AuthResponse;
import com.chathub.modules.auth.dto.LoginRequest;
import com.chathub.modules.auth.dto.SignupRequest;
import com.chathub.modules.auth.dto.UserResponse;
import com.chathub.modules.auth.repository.AuthRepository;
import com.chathub.modules.auth.repository.UserRecord;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Signup: reject duplicate email/username, hash the password (BCrypt, 10
 * rounds), issue a JWT. Login: verify credentials, issue a JWT. Error strings
 * match the Deno backend exactly. Never logs passwords or hashes.
 */
@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final AuthRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthServiceImpl(AuthRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Override
	public AuthResponse signup(SignupRequest request) {
		log.debug("Signup : checking email/username availability for {}", request.email());
		if (repository.findByEmail(request.email()).isPresent()) {
			log.debug("Signup rejected : email already in use");
			throw new DuplicateResourceException("Email already in use");
		}
		if (repository.findByUsername(request.username()).isPresent()) {
			log.debug("Signup rejected : username already in use");
			throw new DuplicateResourceException("Username already in use");
		}
		String passwordHash = passwordEncoder.encode(request.password());
		UserRecord user = repository.createUser(request.username(), request.email(), passwordHash);
		AuthResponse response = toAuthResponse(user);
		LogDetails.with(Map.of("userId", user.id(), "email", user.email()),
				() -> log.info("Signup successful"));
		return response;
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		log.debug("Login : validating credentials for {}", request.email());
		UserRecord user = repository.findByEmail(request.email()).orElseThrow(() -> {
			log.debug("Login failed : no user for email");
			return new InvalidCredentialsException("Invalid credentials");
		});
		if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
			log.debug("Login failed : password mismatch");
			throw new InvalidCredentialsException("Invalid credentials");
		}
		AuthResponse response = toAuthResponse(user);
		LogDetails.with(Map.of("userId", user.id(), "email", user.email()),
				() -> log.info("Login successful"));
		return response;
	}

	private AuthResponse toAuthResponse(UserRecord user) {
		String token = jwtService.createToken(user.id(), user.email());
		return new AuthResponse(new UserResponse(user.id(), user.username(), user.email()), token);
	}
}
