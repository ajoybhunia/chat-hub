package com.chathub.modules.auth.service;

import com.chathub.modules.auth.dto.AuthResponse;
import com.chathub.modules.auth.dto.LoginRequest;
import com.chathub.modules.auth.dto.SignupRequest;

/** Business logic for signup and login. */
public interface AuthService {

	AuthResponse signup(SignupRequest request);

	AuthResponse login(LoginRequest request);
}
