package com.chathub.config;

import com.chathub.websocket.AuthenticatedUserHandshakeInterceptor;
import com.chathub.websocket.ChatWebSocketHandler;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Native WebSocket endpoint at {@code /} (parity with the Deno backend — no
 * STOMP/SockJS). JWT verification happens in {@code WebSocketAuthFilter}
 * before the handshake; browser origins are checked against
 * {@code app.websocket-allowed-origin-patterns} during the handshake (Spring's
 * {@code OriginHandshakeInterceptor} rejects everything else with 403).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final ChatWebSocketHandler chatWebSocketHandler;
	private final AuthenticatedUserHandshakeInterceptor userHandshakeInterceptor;
	private final AppProperties appProperties;

	public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
			AuthenticatedUserHandshakeInterceptor userHandshakeInterceptor, AppProperties appProperties) {
		this.chatWebSocketHandler = chatWebSocketHandler;
		this.userHandshakeInterceptor = userHandshakeInterceptor;
		this.appProperties = appProperties;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		WebSocketHandlerRegistration registration = registry.addHandler(chatWebSocketHandler, "/")
				.addInterceptors(userHandshakeInterceptor);
		java.util.		List<String> allowedOriginPatterns = appProperties.websocketAllowedOriginPatterns();
		if (allowedOriginPatterns != null && !allowedOriginPatterns.isEmpty()) {
			registration.setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new));
		}
	}
}
