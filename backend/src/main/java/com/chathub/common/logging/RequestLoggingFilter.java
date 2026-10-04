package com.chathub.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * One flow line per request: {@code GET /rooms → 401} with
 * {@code details.durationMs}. 2xx/3xx at DEBUG, 4xx at WARN, 5xx at ERROR;
 * CORS preflights (OPTIONS) are always DEBUG to keep the console quiet.
 * Query strings are logged with {@code token=} redacted — JWTs must never
 * reach the logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long start = System.nanoTime();
		try {
			chain.doFilter(request, response);
		} finally {
			logOutcome(request, response.getStatus(), (System.nanoTime() - start) / 1_000_000);
		}
	}

	private static void logOutcome(HttpServletRequest request, int status, long durationMs) {
		String message = request.getMethod() + " : " + sanitizedPath(request) + " → " + status;
		LogDetails.with(java.util.Map.of("durationMs", durationMs), () -> {
			if ("OPTIONS".equals(request.getMethod()) || status < 400) {
				log.debug(message);
			} else if (status < 500) {
				log.warn(message);
			} else {
				log.error(message);
			}
		});
	}

	private static String sanitizedPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		String query = request.getQueryString();
		if (query == null || query.isEmpty()) {
			return path;
		}
		return path + "?" + query.replaceAll("token=[^&]*", "token=***");
	}
}
