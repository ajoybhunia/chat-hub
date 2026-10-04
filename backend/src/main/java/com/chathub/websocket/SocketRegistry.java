package com.chathub.websocket;

import java.io.IOException;
import java.util.Collection;
import org.springframework.web.socket.WebSocketSession;

/** Registry of currently connected WebSocket clients. */
public interface SocketRegistry {

	void add(WebSocketSession session);

	void remove(WebSocketSession session);

	/** Live view of all registered sessions; iteration is thread-safe. */
	Collection<WebSocketSession> all();

	int size();
}
