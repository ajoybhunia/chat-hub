package com.chathub.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chathub.TestcontainersConfiguration;
import com.chathub.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end HTTP parity checks (C1–C8) against a real PostgreSQL container —
 * same wire contract the frontend consumes from the Deno backend.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void c1SignupReturns201WithUserAndToken() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_signup_user","email":"it-signup@example.com","password":"password123"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user.id").isNumber())
				.andExpect(jsonPath("$.user.username").value("it_signup_user"))
				.andExpect(jsonPath("$.user.email").value("it-signup@example.com"))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.password_hash").doesNotExist())
				.andExpect(jsonPath("$.password_hash").doesNotExist());
	}

	@Test
	void c2SignupDuplicateEmailReturns400() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_dup_email_user","email":"it-dup-email@example.com","password":"password123"}"""))
				.andExpect(status().isCreated());
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"other_name","email":"it-dup-email@example.com","password":"password123"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Email already in use"));
	}

	@Test
	void c2SignupDuplicateUsernameReturns400() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_dup_username_user","email":"it-dup-user@example.com","password":"password123"}"""))
				.andExpect(status().isCreated());
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_dup_username_user","email":"other@example.com","password":"password123"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Username already in use"));
	}

	@Test
	void c3LoginReturns200WithUserAndToken() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_login_user","email":"it-login@example.com","password":"password123"}"""))
				.andExpect(status().isCreated());

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"it-login@example.com","password":"password123"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.username").value("it_login_user"))
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void c4LoginFailuresReturn400InvalidCredentials() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"it-ghost@example.com","password":"password123"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid credentials"));

		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_wrongpw_user","email":"it-wrongpw@example.com","password":"password123"}"""))
				.andExpect(status().isCreated());
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"it-wrongpw@example.com","password":"wrongpassword"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid credentials"));
	}

	@Test
	void c5IssuedTokenPassesJwtVerification() throws Exception {
		var result = mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_token_user","email":"it-token@example.com","password":"password123"}"""))
				.andExpect(status().isCreated()).andReturn();

		String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
		var user = jwtService.verify(token);
		assertThat(user.id()).isPositive();
		assertThat(user.email()).isEqualTo("it-token@example.com");
	}

	@Test
	void c6CorsPreflightAllowsFrontendOrigin() throws Exception {
		var result = mvc.perform(options("/auth/login").header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andReturn();

		assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");
		assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).contains("Authorization");
	}

	@Test
	void c7ProtectedRouteWithoutTokenReturns401MissingHeader() throws Exception {
		mvc.perform(get("/rooms"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Missing Authorization header"));
	}

	@Test
	void c8ProtectedRouteWithBadTokenReturns401InvalidToken() throws Exception {
		mvc.perform(get("/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired token"));
	}

	@Test
	void protectedRouteWithValidTokenReachesDispatcher404() throws Exception {
		String token = jwtService.createToken(1, "anyone@example.com");
		mvc.perform(get("/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not found"));
	}

	@Test
	void validationFailureReturns400WithFieldMessage() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"it_bad","email":"not-an-email","password":"password123"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("email must be a well-formed email address"));
	}

	@Test
	void wrongMethodUnderAuthReturns404NotFound() throws Exception {
		mvc.perform(get("/auth/login"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not found"));
	}
}
