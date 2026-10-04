package com.chathub.common.security;

import com.chathub.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Signs and verifies JWTs compatible with the Deno backend: HS256, payload
 * {@code {id, email}}, issued-at now, 7-day expiry, secret from JWT_SECRET.
 */
@Service
public class JwtService {

	static final Duration TOKEN_TTL = Duration.ofDays(7);

	private final SecretKey key;

	public JwtService(AppProperties properties) {
		this.key = deriveKey(properties.jwtSecret());
	}

	public String createToken(int id, String email) {
		Instant now = Instant.now();
		return Jwts.builder()
				.claim("id", id)
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(TOKEN_TTL)))
				// Explicit HS256 — jjwt would pick HS512 for long secrets, but the
				// Deno backend always signs HS256.
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public AuthenticatedUser verify(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			Number id = claims.get("id", Number.class);
			if (id == null) {
				throw new InvalidTokenException("token is missing the id claim");
			}
			return new AuthenticatedUser(id.intValue(), claims.get("email", String.class));
		} catch (InvalidTokenException e) {
			throw e;
		} catch (JwtException | IllegalArgumentException e) {
			throw new InvalidTokenException("invalid token", e);
		}
	}

	/**
	 * jjwt enforces >= 256-bit HMAC keys; shorter secrets (accepted by the Deno
	 * backend) are SHA-256 hashed so any JWT_SECRET works.
	 */
	static SecretKey deriveKey(String secret) {
		byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
		if (raw.length >= 32) {
			return Keys.hmacShaKeyFor(raw);
		}
		try {
			return Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-256").digest(raw));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}
}
