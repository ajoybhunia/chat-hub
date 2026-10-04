package com.chathub.common.security;

import com.chathub.common.error.ApiError;
import com.chathub.common.logging.LogDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * HTTP port of the Deno {@code requireAuth()} guard: everything outside
 * {@code /auth/**} needs {@code Authorization: Bearer <jwt>}. WebSocket upgrades
 * are skipped — they authenticate via {@code ?token=} in the handshake
 * interceptor instead. Runs after the CORS filter, so responses written here
 * already carry CORS headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final ObjectMapper objectMapper;

	public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if ("OPTIONS".equals(request.getMethod())) {
			return true;
		}
		if (request.getRequestURI().startsWith("/auth/")) {
			return true;
		}
		String upgrade = request.getHeader("Upgrade");
		return upgrade != null && upgrade.equalsIgnoreCase("websocket");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		log.debug("Authorizing : {} {}", request.getMethod(), request.getRequestURI());
		String header = request.getHeader("Authorization");
		if (header == null || header.isEmpty()) {
			reject(request, response, "Missing Authorization header");
			return;
		}
		String token = header.startsWith(BEARER_PREFIX) ? header.substring(BEARER_PREFIX.length()) : header;
		try {
			AuthenticatedUser user = jwtService.verify(token);
			request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
			chain.doFilter(request, response);
		} catch (InvalidTokenException e) {
			reject(request, response, "Invalid or expired token");
		}
	}

	private void reject(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
		LogDetails.with(Map.of("method", request.getMethod(), "path", request.getRequestURI(), "reason", message),
				() -> log.warn("Authorization failed"));
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(new ApiError(message)));
	}
}
