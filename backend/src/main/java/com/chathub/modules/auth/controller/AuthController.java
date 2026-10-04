package com.chathub.modules.auth.controller;

import com.chathub.modules.auth.dto.AuthResponse;
import com.chathub.modules.auth.dto.LoginRequest;
import com.chathub.modules.auth.dto.SignupRequest;
import com.chathub.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for authentication. Status codes mirror the Deno backend:
 * signup → 201, login → 200, failures → 400 (mapped in
 * {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
		log.debug("POST : /auth/signup");
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		log.debug("POST : /auth/login");
		return authService.login(request);
	}
}
