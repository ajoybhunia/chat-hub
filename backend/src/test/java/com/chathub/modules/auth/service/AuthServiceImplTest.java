package com.chathub.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chathub.common.error.DuplicateResourceException;
import com.chathub.common.error.InvalidCredentialsException;
import com.chathub.common.security.JwtService;
import com.chathub.modules.auth.dto.AuthResponse;
import com.chathub.modules.auth.dto.LoginRequest;
import com.chathub.modules.auth.dto.SignupRequest;
import com.chathub.modules.auth.repository.AuthRepository;
import com.chathub.modules.auth.repository.UserRecord;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private AuthRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthServiceImpl service;

	@Test
	void signupHashesPasswordAndReturnsToken() {
		SignupRequest request = new SignupRequest("newuser", "new@example.com", "password123");
		when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
		when(repository.findByUsername("newuser")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
		when(repository.createUser("newuser", "new@example.com", "$2a$10$hashed"))
				.thenReturn(new UserRecord(11, "newuser", "new@example.com", "$2a$10$hashed"));
		when(jwtService.createToken(11, "new@example.com")).thenReturn("signed-token");

		AuthResponse response = service.signup(request);

		verify(repository).createUser("newuser", "new@example.com", "$2a$10$hashed");
		assertThat(response.token()).isEqualTo("signed-token");
		assertThat(response.user().id()).isEqualTo(11);
		assertThat(response.user().email()).isEqualTo("new@example.com");
	}

	@Test
	void signupRejectsDuplicateEmail() {
		when(repository.findByEmail("taken@example.com")).thenReturn(Optional.of(new UserRecord(1, "owner", "taken@example.com", "hash")));

		assertThatThrownBy(() -> service.signup(new SignupRequest("someone", "taken@example.com", "password123")))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Email already in use");
		verify(repository, never()).createUser(anyString(), anyString(), anyString());
	}

	@Test
	void signupRejectsDuplicateUsername() {
		when(repository.findByEmail("free@example.com")).thenReturn(Optional.empty());
		when(repository.findByUsername("takenuser")).thenReturn(Optional.of(new UserRecord(2, "takenuser", "other@example.com", "hash")));

		assertThatThrownBy(() -> service.signup(new SignupRequest("takenuser", "free@example.com", "password123")))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Username already in use");
		verify(repository, never()).createUser(anyString(), anyString(), anyString());
	}

	@Test
	void loginReturnsTokenForValidCredentials() {
		when(repository.findByEmail("valid@example.com"))
				.thenReturn(Optional.of(new UserRecord(5, "valid", "valid@example.com", "$2a$10$hash")));
		when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);
		when(jwtService.createToken(5, "valid@example.com")).thenReturn("login-token");

		AuthResponse response = service.login(new LoginRequest("valid@example.com", "password123"));

		assertThat(response.token()).isEqualTo("login-token");
		assertThat(response.user().username()).isEqualTo("valid");
	}

	@Test
	void loginRejectsUnknownEmail() {
		when(repository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "password123")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid credentials");
		verify(jwtService, never()).createToken(org.mockito.ArgumentMatchers.anyInt(), anyString());
	}

	@Test
	void loginRejectsWrongPassword() {
		when(repository.findByEmail("valid@example.com"))
				.thenReturn(Optional.of(new UserRecord(5, "valid", "valid@example.com", "$2a$10$hash")));
		when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

		assertThatThrownBy(() -> service.login(new LoginRequest("valid@example.com", "wrong")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid credentials");
		verify(jwtService, never()).createToken(org.mockito.ArgumentMatchers.anyInt(), anyString());
	}
}
