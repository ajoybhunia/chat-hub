package com.chathub.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

	private static final Pattern ISO_MS_UTC = Pattern
			.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");

	private final InMemorySocketRegistry registry = new InMemorySocketRegistry();
	private final ChatWebSocketHandler handler = new ChatWebSocketHandler(registry, new ObjectMapper());
	private final ObjectMapper mapper = new ObjectMapper();

	@Mock
	private WebSocketSession sender;

	@Mock
	private WebSocketSession receiver;

	@Test
	void broadcastsToAllOpenClientsIncludingSender() throws Exception {
		when(sender.isOpen()).thenReturn(true);
		when(receiver.isOpen()).thenReturn(true);
		handler.afterConnectionEstablished(sender);
		handler.afterConnectionEstablished(receiver);

		handler.handleTextMessage(sender, new TextMessage("{\"user\":\"Alice\",\"content\":\"hello\"}"));

		var payload = sentPayload(receiver);
		assertThat(payload.get("user").asString()).isEqualTo("Alice");
		assertThat(payload.get("content").asString()).isEqualTo("hello");
		assertThat(payload.get("timestamp").asString()).matches(ISO_MS_UTC.pattern());
		// Deno broadcasts to ALL clients — sender included
		verify(sender).sendMessage(any());
	}

	@Test
	void defaultsMissingUserToAnonymous() throws Exception {
		when(sender.isOpen()).thenReturn(true);
		handler.afterConnectionEstablished(sender);

		handler.handleTextMessage(sender, new TextMessage("{\"content\":\"no user field\"}"));

		assertThat(sentPayload(sender).get("user").asString()).isEqualTo("Anonymous");
	}

	@Test
	void ignoresMalformedFrameAndKeepsConnection() throws Exception {
		handler.afterConnectionEstablished(sender);

		handler.handleTextMessage(sender, new TextMessage("this is not json {"));

		verify(sender, never()).sendMessage(any());
		verify(receiver, never()).sendMessage(any());
		assertThat(registry.size()).isEqualTo(1);
	}

	@Test
	void skipsClosedClients() throws Exception {
		when(sender.isOpen()).thenReturn(true);
		when(receiver.isOpen()).thenReturn(false);
		handler.afterConnectionEstablished(sender);
		handler.afterConnectionEstablished(receiver);

		handler.handleTextMessage(sender, new TextMessage("{\"user\":\"Alice\",\"content\":\"hi\"}"));

		verify(receiver, never()).sendMessage(any());
	}

	@Test
	void tracksConnectionsInRegistry() {
		handler.afterConnectionEstablished(sender);
		assertThat(registry.size()).isEqualTo(1);

		handler.afterConnectionClosed(sender, null);
		assertThat(registry.size()).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	private tools.jackson.databind.JsonNode sentPayload(WebSocketSession session) throws Exception {
		ArgumentCaptor<WebSocketMessage<?>> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
		verify(session).sendMessage(captor.capture());
		return mapper.readTree(((TextMessage) captor.getValue()).getPayload());
	}
}
