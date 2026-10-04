package com.chathub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/**
 * The processor is what makes every launch path (bootRun, IntelliJ Run,
 * java -jar) see the same DATABASE_URL/JWT_SECRET, and what turns Hikari's
 * cryptic 'url' must start with "jdbc" into an actionable startup error.
 * Tests mirror application.yml by seeding the placeholder properties the same
 * way the real config does.
 */
class DatabaseUrlEnvironmentPostProcessorTest {

	@TempDir
	Path tempDir;

	private final MockEnvironment environment = new MockEnvironment();

	/** Mirrors application.yml: both values are placeholders fed by the environment. */
	private void mirrorApplicationYml() {
		environment.setProperty("spring.datasource.url", "${DATABASE_URL}");
		environment.setProperty("app.jwt-secret", "${JWT_SECRET}");
	}

	private DatabaseUrlEnvironmentPostProcessor processorWithoutDotEnv() {
		return new DatabaseUrlEnvironmentPostProcessor(tempDir.resolve("no-such-file"));
	}

	@Test
	void missingVariablesFailFastWithActionableMessage() {
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");

		assertThatThrownBy(() -> processorWithoutDotEnv().postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("DATABASE_URL is not set")
				.hasMessageContaining(".env");
	}

	@Test
	void missingJwtSecretFailsFast() {
		mirrorApplicationYml();
		environment.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/app");

		assertThatThrownBy(() -> processorWithoutDotEnv().postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET is not set");
	}

	@Test
	void convertsPostgresUrlFromEnvironment() {
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");
		environment.setProperty("DATABASE_URL", "postgres://chatuser:chatpass@localhost:5433/chatdb");

		processorWithoutDotEnv().postProcessEnvironment(environment, null);

		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://localhost:5433/chatdb");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("chatuser");
		assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("chatpass");
	}

	@Test
	void jdbcFormPassesThroughUntouched() {
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");
		environment.setProperty("DATABASE_URL", "jdbc:postgresql://db:5432/app?user=u&password=p");

		processorWithoutDotEnv().postProcessEnvironment(environment, null);

		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://db:5432/app?user=u&password=p");
		assertThat(environment.getProperty("spring.datasource.username")).isNull();
	}

	@Test
	void invalidSchemeFailsFastWithoutEchoingCredentials() {
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");
		environment.setProperty("DATABASE_URL", "mysql://user:secret@host:3306/db");

		assertThatThrownBy(() -> processorWithoutDotEnv().postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("must start with")
				.hasMessageNotContaining("secret");
	}

	@Test
	void dotEnvFillsMissingVariables() throws IOException {
		Files.writeString(tempDir.resolve(".env"), """
				# local dev secrets
				DATABASE_URL="postgres://chatuser:chatpass@localhost:5433/chatdb"

				JWT_SECRET=file-secret-0123456789
				""");
		mirrorApplicationYml();

		new DatabaseUrlEnvironmentPostProcessor(tempDir.resolve(".env")).postProcessEnvironment(environment, null);

		assertThat(environment.getProperty("JWT_SECRET")).isEqualTo("file-secret-0123456789");
		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://localhost:5433/chatdb");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("chatuser");
		assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("chatpass");
	}

	@Test
	void environmentWinsOverDotEnv() throws IOException {
		Files.writeString(tempDir.resolve(".env"),
				"DATABASE_URL=postgres://fileuser:filepass@file-host:5433/filedb\nJWT_SECRET=file-secret\n");
		mirrorApplicationYml();
		environment.setProperty("DATABASE_URL", "postgres://osuser:ospass@os-host:5433/osdb");
		environment.setProperty("JWT_SECRET", "os-secret-0123456789");

		new DatabaseUrlEnvironmentPostProcessor(tempDir.resolve(".env")).postProcessEnvironment(environment, null);

		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://os-host:5433/osdb");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("osuser");
		assertThat(environment.getProperty("JWT_SECRET")).isEqualTo("os-secret-0123456789");
	}

	@Test
	void existingJdbcUrlSkipsValidation() {
		// Testcontainers / SPRING_DATASOURCE_URL path: nothing to convert, nothing to enforce.
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");
		environment.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/overridden");

		assertThatNoException().isThrownBy(() -> processorWithoutDotEnv().postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://localhost:5432/overridden");
	}

	@Test
	void missingDatabaseNameFailsWithoutLeakingCredentials() {
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");
		environment.setProperty("DATABASE_URL", "postgres://user:secret@dbhost");

		assertThatThrownBy(() -> processorWithoutDotEnv().postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing a database name")
				.hasMessageNotContaining("secret");
	}

	@Test
	void invalidUriSyntaxFailsWithoutLeakingCredentials() {
		mirrorApplicationYml();
		environment.setProperty("JWT_SECRET", "provided-secret-0123456789");
		environment.setProperty("DATABASE_URL", "postgres://user:se cret@host:5432/db");

		assertThatThrownBy(() -> processorWithoutDotEnv().postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("DATABASE_URL")
				.hasMessageNotContaining("se cret");
	}
}
