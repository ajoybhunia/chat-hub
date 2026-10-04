package com.chathub.websocket;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * In-memory registry (parity with the Deno backend's {@code Set<WebSocket>}).
 * A future Redis-backed presence implementation (issue #14) can implement
 * {@link SocketRegistry} alongside it.
 */
@Component
public class InMemorySocketRegistry implements SocketRegistry {

	private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

	@Override
	public void add(WebSocketSession session) {
		sessions.add(session);
	}

	@Override
	public void remove(WebSocketSession session) {
		sessions.remove(session);
	}

	@Override
	public Collection<WebSocketSession> all() {
		return sessions;
	}

	@Override
	public int size() {
		return sessions.size();
	}
}
