package com.chathub.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chathub.TestcontainersConfiguration;
import com.chathub.common.security.JwtService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * WebSocket parity (C9–C12): pre-upgrade 401s via MockMvc, real handshake and
 * broadcast between two live clients on a random port.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebSocketIntegrationTest {

	private static final Pattern ISO_MS_UTC = Pattern
			.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");

	@Autowired
	private org.springframework.test.web.servlet.MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@LocalServerPort
	private int port;

	@Test
	void c9UpgradeWithoutTokenReturns401MissingHeader() throws Exception {
		mvc.perform(get("/").header("Upgrade", "websocket").header("Connection", "Upgrade")
				.header("Sec-WebSocket-Version", "13").header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ=="))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Missing Authorization header"));
	}

	@Test
	void c10UpgradeWithInvalidTokenReturns401InvalidToken() throws Exception {
		mvc.perform(get("/?token=not.a.token").header("Upgrade", "websocket").header("Connection", "Upgrade")
				.header("Sec-WebSocket-Version", "13").header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ=="))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired token"));
	}

	@Test
	void c11BroadcastsMessageToAllClientsWithIsoTimestamp() throws Exception {
		String token = jwtService.createToken(1, "ws@example.com");
		ClientEndpoint alice = connect(token);
		ClientEndpoint bob = connect(token);

		alice.session.get().sendMessage(new TextMessage("{\"user\":\"Alice\",\"content\":\"hello parity\"}"));

		String payload = bob.inbox.poll(5, TimeUnit.SECONDS);
		assertThat(payload).isNotNull();
		var json = new tools.jackson.databind.ObjectMapper().readTree(payload);
		assertThat(json.get("user").asString()).isEqualTo("Alice");
		assertThat(json.get("content").asString()).isEqualTo("hello parity");
		assertThat(json.get("timestamp").asString()).matches(ISO_MS_UTC.pattern());

		// sender receives its own broadcast too (Deno parity)
		assertThat(alice.inbox.poll(5, TimeUnit.SECONDS)).isNotNull();

		alice.close();
		bob.close();
	}

	@Test
	void c11bMissingUserFieldDefaultsToAnonymous() throws Exception {
		String token = jwtService.createToken(1, "ws@example.com");
		ClientEndpoint alice = connect(token);
		ClientEndpoint bob = connect(token);

		alice.session.get().sendMessage(new TextMessage("{\"content\":\"anon\"}"));

		String payload = bob.inbox.poll(5, TimeUnit.SECONDS);
		var json = new tools.jackson.databind.ObjectMapper().readTree(payload);
		assertThat(json.get("user").asString()).isEqualTo("Anonymous");

		alice.close();
		bob.close();
	}

	@Test
	void c12MalformedFrameIsIgnoredAndConnectionStaysOpen() throws Exception {
		String token = jwtService.createToken(1, "ws@example.com");
		ClientEndpoint alice = connect(token);
		ClientEndpoint bob = connect(token);

		alice.session.get().sendMessage(new TextMessage("this is not json {"));

		assertThat(bob.inbox.poll(500, TimeUnit.MILLISECONDS)).isNull();
		assertThat(alice.session.get().isOpen()).isTrue();

		// connection still works afterwards
		alice.session.get().sendMessage(new TextMessage("{\"user\":\"Alice\",\"content\":\"still alive\"}"));
		assertThat(bob.inbox.poll(5, TimeUnit.SECONDS)).isNotNull();

		alice.close();
		bob.close();
	}

	@Test
	void c13BrowserOriginFromFrontendIsAllowed() throws Exception {
		assertThat(handshakeStatus("http://localhost:5173")).isEqualTo(101);
	}

	@Test
	void c13bNoOriginHeaderStillHandshakesForNonBrowserClients() throws Exception {
		assertThat(handshakeStatus(null)).isEqualTo(101);
	}

	@Test
	void c13cCrossSiteOriginIsRejectedWith403() throws Exception {
		assertThat(handshakeStatus("http://evil.example")).isEqualTo(403);
	}

	private ClientEndpoint connect(String token) throws Exception {
		ClientEndpoint endpoint = new ClientEndpoint();
		CompletableFuture<WebSocketSession> future = new StandardWebSocketClient()
				.execute(endpoint, null, URI.create("ws://localhost:" + port + "/?token=" + token));
		endpoint.session = future;
		return endpoint;
	}

	private int handshakeStatus(String origin) throws Exception {
		String token = jwtService.createToken(1, "ws@example.com");
		try (Socket socket = new Socket("localhost", port)) {
			String originLine = origin == null ? "" : "Origin: " + origin + "\r\n";
			String request = "GET /?token=" + token + " HTTP/1.1\r\n"
					+ "Host: localhost:" + port + "\r\n"
					+ "Upgrade: websocket\r\n"
					+ "Connection: Upgrade\r\n"
					+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
					+ "Sec-WebSocket-Version: 13\r\n"
					+ originLine + "\r\n";
			socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
			socket.getOutputStream().flush();
			socket.setSoTimeout(5000);
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
					StandardCharsets.UTF_8));
			String statusLine = reader.readLine();
			assertThat(statusLine).isNotNull();
			return Integer.parseInt(statusLine.split(" ")[1]);
		}
	}

	private static final class ClientEndpoint extends org.springframework.web.socket.handler.TextWebSocketHandler {

		private final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
		private CompletableFuture<WebSocketSession> session;

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			inbox.add(message.getPayload());
		}

		void close() throws Exception {
			WebSocketSession open = session.get(1, TimeUnit.SECONDS);
			if (open.isOpen()) {
				open.close();
			}
		}
	}
}
