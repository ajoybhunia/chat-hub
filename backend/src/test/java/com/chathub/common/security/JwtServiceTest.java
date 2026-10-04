package com.chathub.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chathub.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String SECRET = "unit-test-secret-that-is-long-enough-32-bytes";

	private final JwtService service = new JwtService(new AppProperties(SECRET, List.of()));

	@Test
	void createTokenProducesHs256WithIdAndEmailClaims() {
		String token = service.createToken(42, "ajo@example.com");

		String headerJson = decodeSegment(token, 0);
		assertThat(headerJson).contains("\"alg\":\"HS256\"");

		String payloadJson = decodeSegment(token, 1);
		assertThat(payloadJson).contains("\"id\":42").contains("\"email\":\"ajo@example.com\"");

		// 7-day expiry
		Instant issuedAt = Instant.ofEpochSecond(extractNumber(payloadJson, "iat"));
		Instant expiration = Instant.ofEpochSecond(extractNumber(payloadJson, "exp"));
		assertThat(expiration.getEpochSecond() - issuedAt.getEpochSecond()).isEqualTo(7 * 24 * 3600);
	}

	@Test
	void verifyRoundTripsTokenClaims() {
		AuthenticatedUser user = service.verify(service.createToken(7, "round@trip.com"));

		assertThat(user.id()).isEqualTo(7);
		assertThat(user.email()).isEqualTo("round@trip.com");
	}

	@Test
	void verifyRejectsExpiredToken() {
		String expired = Jwts.builder()
				.claim("id", 1)
				.claim("email", "expired@example.com")
				.issuedAt(Date.from(Instant.now().minusSeconds(8 * 24 * 3600)))
				.expiration(Date.from(Instant.now().minusSeconds(60)))
				.signWith(JwtService.deriveKey(SECRET), Jwts.SIG.HS256)
				.compact();

		assertThatThrownBy(() -> service.verify(expired)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void verifyRejectsTokenSignedWithDifferentSecret() {
		String foreign = Jwts.builder()
				.claim("id", 1)
				.claim("email", "attacker@example.com")
				.expiration(Date.from(Instant.now().plusSeconds(3600)))
				.signWith(Keys.hmacShaKeyFor("a-different-secret-that-is-also-long-enough!".getBytes(StandardCharsets.UTF_8)),
						Jwts.SIG.HS256)
				.compact();

		assertThatThrownBy(() -> service.verify(foreign)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void verifyRejectsGarbageAndTamperedTokens() {
		assertThatThrownBy(() -> service.verify("not.a.jwt")).isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> service.verify("")).isInstanceOf(InvalidTokenException.class);

		String valid = service.createToken(1, "tamper@example.com");
		String tampered = valid.substring(0, valid.length() - 2) + "xy";
		assertThatThrownBy(() -> service.verify(tampered)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void verifyRejectsTokenMissingIdClaim() {
		String noId = Jwts.builder()
				.claim("email", "noid@example.com")
				.expiration(Date.from(Instant.now().plusSeconds(3600)))
				.signWith(JwtService.deriveKey(SECRET), Jwts.SIG.HS256)
				.compact();

		assertThatThrownBy(() -> service.verify(noId)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void shortSecretsAreHashedSoAnyJwtSecretWorks() {
		// Deno's .env ships a 31-char secret; jjwt needs >= 32 bytes
		JwtService shortSecretService = new JwtService(new AppProperties("deno-style-31-char-secret!!", List.of()));

		AuthenticatedUser user = shortSecretService.verify(shortSecretService.createToken(3, "short@secret.com"));

		assertThat(user.id()).isEqualTo(3);
	}

	private static String decodeSegment(String token, int index) {
		return new String(Base64.getUrlDecoder().decode(token.split("\\.")[index]), StandardCharsets.UTF_8);
	}

	private static long extractNumber(String json, String field) {
		int start = json.indexOf("\"" + field + "\":") + field.length() + 3;
		int end = start;
		while (end < json.length() && Character.isDigit(json.charAt(end))) {
			end++;
		}
		return Long.parseLong(json.substring(start, end));
	}
}
