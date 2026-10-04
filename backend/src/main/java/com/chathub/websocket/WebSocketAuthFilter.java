package com.chathub.websocket;

import com.chathub.common.error.ApiError;
import com.chathub.common.security.AuthenticatedUser;
import com.chathub.common.security.InvalidTokenException;
import com.chathub.common.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies the {@code ?token=} query parameter on WebSocket upgrade requests
 * BEFORE the handshake — parity with the Deno request handler, which checks the
 * token before upgrading. Rejections return the same 401 JSON bodies as the
 * HTTP auth filter. On success the authenticated user is placed on a request
 * attribute for {@link AuthenticatedUserHandshakeInterceptor} to copy into the
 * session attributes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class WebSocketAuthFilter extends OncePerRequestFilter {

	public static final String USER_ATTRIBUTE = "chathub.authenticatedUser";

	private static final Logger log = LoggerFactory.getLogger(WebSocketAuthFilter.class);

	private final JwtService jwtService;
	private final ObjectMapper objectMapper;

	public WebSocketAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		boolean isUpgrade = "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
		return !"GET".equals(request.getMethod()) || !isUpgrade;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = queryParam(request, "token");
		if (!StringUtils.hasText(token)) {
			reject(response, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
			return;
		}
		try {
			AuthenticatedUser user = jwtService.verify(token);
			request.setAttribute(USER_ATTRIBUTE, user);
		} catch (InvalidTokenException e) {
			reject(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static String queryParam(HttpServletRequest request, String name) {
		String query = request.getQueryString();
		if (query == null) {
			return null;
		}
		for (String pair : query.split("&")) {
			int eq = pair.indexOf('=');
			String key = eq < 0 ? pair : pair.substring(0, eq);
			if (name.equals(key)) {
				return eq < 0 ? "" : java.net.URLDecoder.decode(pair.substring(eq + 1),
						java.nio.charset.StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		log.warn("WebSocket upgrade rejected : {}", message);
		response.setStatus(status.value());
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(new ApiError(message)));
	}
}
