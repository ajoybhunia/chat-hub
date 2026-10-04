package com.chathub.config;

import java.net.URI;

/**
 * Converts {@code postgres://user:pass@host:5432/db} URLs (the format used by
 * docker-compose and the Deno backend) into the JDBC form required by the
 * PostgreSQL driver, extracting credentials along the way. JDBC-form URLs pass
 * through unchanged so they may carry their own {@code ?user=...&password=...}.
 */
public final class DatabaseUrls {

	/** Result of parsing a DATABASE_URL: JDBC URL plus optional credentials. */
	public record JdbcTarget(String url, String username, String password) {
	}

	private DatabaseUrls() {
	}

	public static boolean isPostgresProtocol(String url) {
		return url.startsWith("postgres://") || url.startsWith("postgresql://");
	}

	public static JdbcTarget convert(String rawUrl) {
		if (!isPostgresProtocol(rawUrl)) {
			return new JdbcTarget(rawUrl, null, null);
		}
		URI uri;
		try {
			uri = URI.create(rawUrl);
		} catch (IllegalArgumentException e) {
			// URI.create echoes the raw URL (credentials included) — keep it out of logs.
			throw new IllegalArgumentException("DATABASE_URL is not a valid URL");
		}
		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			// Never echo rawUrl: it carries credentials and lands in the log stream.
			throw new IllegalArgumentException("DATABASE_URL is missing a host");
		}
		int port = uri.getPort() == -1 ? 5432 : uri.getPort();
		String database = uri.getPath();
		if (database == null || database.isBlank() || database.equals("/")) {
			throw new IllegalArgumentException("DATABASE_URL is missing a database name");
		}
		String username = null;
		String password = null;
		String userInfo = uri.getUserInfo();
		if (userInfo != null) {
			int separator = userInfo.indexOf(':');
			username = separator >= 0 ? userInfo.substring(0, separator) : userInfo;
			password = separator >= 0 ? userInfo.substring(separator + 1) : null;
		}
		String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + database;
		return new JdbcTarget(jdbcUrl, emptyToNull(username), emptyToNull(password));
	}

	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}
}
