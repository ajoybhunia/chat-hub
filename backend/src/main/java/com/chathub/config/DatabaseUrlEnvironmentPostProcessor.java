package com.chathub.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.PlaceholderResolutionException;

/**
 * Gives every launch path — {@code ./gradlew bootRun}, the IntelliJ Run button,
 * {@code java -jar} — the same environment the Deno backend got from {@code --env}:
 *
 * <ol>
 *   <li>{@code backend/.env} fills in any variable not already set in the OS
 *       environment (lowest precedence — real env vars always win).</li>
 *   <li>{@code postgres://user:pass@host:5432/db} DATABASE_URL values are rewritten
 *       into {@code spring.datasource.url/username/password} before the DataSource
 *       is created; JDBC-form URLs pass through untouched.</li>
 *   <li>Missing {@code DATABASE_URL}/{@code JWT_SECRET} abort startup with an
 *       actionable message instead of Hikari's {@code 'url' must start with
 *       "jdbc"} (Deno {@code env.ts} parity).</li>
 * </ol>
 *
 * Registered in {@code META-INF/spring.factories}. Runs after config data
 * (application.yml) so {@code spring.datasource.url} is visible; when that value
 * is already a {@code jdbc:} URL — tests with Testcontainers, or an explicit
 * {@code SPRING_DATASOURCE_URL} — conversion and validation are skipped.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String PROPERTY_SOURCE_NAME = "convertedDatabaseUrl";
	private static final String DOTENV_SOURCE_NAME = "dotenv";

	private static final String MISSING_JWT = "JWT_SECRET is not set. Export it or add it to backend/.env "
			+ "(see .env.example).";
	private static final String MISSING_URL = "DATABASE_URL is not set. Export it or add it to backend/.env "
			+ "(see .env.example).";
	private static final String BAD_URL = "DATABASE_URL must start with postgres://, postgresql:// or jdbc:.";

	private Path envFile = Path.of(".env");

	public DatabaseUrlEnvironmentPostProcessor() {
	}

	/** Non-default constructor is for tests that need a fixture .env path. */
	DatabaseUrlEnvironmentPostProcessor(Path envFile) {
		this.envFile = envFile;
	}

	@Override
	public int getOrder() {
		// After ConfigDataEnvironmentPostProcessor (LOWEST_PRECEDENCE - 10) so that
		// application.yml's spring.datasource.url placeholder is already resolvable.
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		loadDotEnv(environment);

		String jwtSecret = resolveOrEmpty(environment, "app.jwt-secret");
		if (jwtSecret.isBlank() || jwtSecret.startsWith("${")) {
			throw new IllegalStateException(MISSING_JWT);
		}

		String url = resolveOrEmpty(environment, "spring.datasource.url");
		if (url.startsWith("jdbc:")) {
			return;
		}

		String raw = resolveOrEmpty(environment, "DATABASE_URL").trim();
		if (raw.isEmpty()) {
			throw new IllegalStateException(MISSING_URL);
		}
		if (DatabaseUrls.isPostgresProtocol(raw)) {
			DatabaseUrls.JdbcTarget target = DatabaseUrls.convert(raw);
			Map<String, Object> properties = new HashMap<>();
			properties.put("spring.datasource.url", target.url());
			if (target.username() != null) {
				properties.put("spring.datasource.username", target.username());
			}
			if (target.password() != null) {
				properties.put("spring.datasource.password", target.password());
			}
			// addFirst: wins over the application.yml placeholder, which only seeds a default
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
		} else if (!raw.startsWith("jdbc:")) {
			throw new IllegalStateException(BAD_URL);
		}

		if (!resolveOrEmpty(environment, "spring.datasource.url").startsWith("jdbc:")) {
			throw new IllegalStateException(BAD_URL);
		}
	}

	/**
	 * Spring Framework 7 resolves placeholders strictly: an unresolvable
	 * {@code ${DATABASE_URL}} throws instead of returning the literal text. An
	 * empty result here means "variable not provided" — callers fail fast with
	 * an actionable message rather than letting the literal reach Hikari.
	 */
	private static String resolveOrEmpty(ConfigurableEnvironment environment, String key) {
		try {
			return environment.getProperty(key, "");
		} catch (PlaceholderResolutionException e) {
			return "";
		}
	}

	/**
	 * Merges {@code .env} entries that are not already resolvable (OS env wins)
	 * into the lowest-precedence property source, so {@code ${DATABASE_URL}} and
	 * friends resolve no matter how the JVM was launched.
	 */
	private void loadDotEnv(ConfigurableEnvironment environment) {
		if (!Files.isRegularFile(envFile)) {
			return;
		}
		Map<String, Object> values = new HashMap<>();
		try {
			for (String line : Files.readAllLines(envFile)) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				int separator = trimmed.indexOf('=');
				if (separator <= 0) {
					continue;
				}
				String key = trimmed.substring(0, separator).trim();
				if (environment.getProperty(key) != null) {
					continue;
				}
				String value = trimmed.substring(separator + 1).trim();
				if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}
				values.put(key, value);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + envFile.toAbsolutePath(), e);
		}
		if (!values.isEmpty()) {
			environment.getPropertySources().addLast(new MapPropertySource(DOTENV_SOURCE_NAME, values));
		}
	}
}
