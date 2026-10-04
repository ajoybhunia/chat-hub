package com.chathub.websocket;

import com.chathub.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Copies the {@link AuthenticatedUser} verified by
 * {@link WebSocketAuthFilter} from the HTTP request into the WebSocket session
 * attributes, so the message handler can identify the sender.
 */
@Component
public class AuthenticatedUserHandshakeInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		if (request instanceof ServletServerHttpRequest servletRequest) {
			Object user = servletRequest.getServletRequest().getAttribute(WebSocketAuthFilter.USER_ATTRIBUTE);
			if (user instanceof AuthenticatedUser authenticatedUser) {
				attributes.put(WebSocketAuthFilter.USER_ATTRIBUTE, authenticatedUser);
			}
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}
}
