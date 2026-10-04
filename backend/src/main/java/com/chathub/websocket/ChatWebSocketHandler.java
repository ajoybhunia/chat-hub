package com.chathub.websocket;

import com.chathub.common.util.IsoTimestamps;
import com.chathub.websocket.dto.InboundChatMessage;
import com.chathub.websocket.dto.OutboundChatMessage;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles the chat protocol (parity with Deno {@code socket_handler.ts}):
 * broadcast every well-formed message to ALL connected clients as
 * {@code {user, content, timestamp}} (ISO-8601 ms UTC), defaulting {@code user}
 * to "Anonymous". Malformed frames are logged and ignored — the connection
 * stays open.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

	private final SocketRegistry registry;
	private final ObjectMapper objectMapper;

	public ChatWebSocketHandler(SocketRegistry registry, ObjectMapper objectMapper) {
		this.registry = registry;
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		registry.add(session);
		log.info("Client connected. Total clients: {}", registry.size());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		try {
			InboundChatMessage inbound = objectMapper.readValue(message.getPayload(), InboundChatMessage.class);
			String user = (inbound.user() == null || inbound.user().isBlank()) ? "Anonymous" : inbound.user();
			String payload = objectMapper
					.writeValueAsString(new OutboundChatMessage(user, inbound.content(), IsoTimestamps.now()));
			broadcast(payload);
		} catch (Exception e) {
			log.error("Parsing error: {}", e.getMessage());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		registry.remove(session);
		log.info("Client disconnected. Remaining clients: {}", registry.size());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		registry.remove(session);
		log.warn("Transport error: {}", exception.getMessage());
		try {
			if (session.isOpen()) {
				session.close(CloseStatus.SERVER_ERROR);
			}
		} catch (IOException ignored) {
			// already closing
		}
	}

	private void broadcast(String payload) {
		log.info("Broadcast: {}", payload);
		TextMessage text = new TextMessage(payload);
		for (WebSocketSession client : registry.all()) {
			if (!client.isOpen()) {
				continue;
			}
			try {
				client.sendMessage(text);
			} catch (IOException e) {
				log.warn("Failed to send to client {}: {}", client.getId(), e.getMessage());
			}
		}
	}
}
