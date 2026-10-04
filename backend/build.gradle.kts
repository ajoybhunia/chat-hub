plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.chathub"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// --- main ---
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("org.postgresql:postgresql")

	// --- tests ---
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Local dev .env loading lives in DatabaseUrlEnvironmentPostProcessor, which
// reads .env (project dir) at Spring startup so every launch path (bootRun,
// IntelliJ Run, java -jar) gets DATABASE_URL/JWT_SECRET. Tests ignore .env —
// they run against Testcontainers and src/test/resources/application.yml.

tasks.withType<Test> {
	useJUnitPlatform()

	// Colima compatibility: Testcontainers looks for /var/run/docker.sock by default,
	// but Colima keeps its socket at ~/.colima/default/docker.sock. Only applied
	// when DOCKER_HOST is not already set by the caller.
	if (System.getenv("DOCKER_HOST") == null) {
		val colimaSocket = file("${System.getProperty("user.home")}/.colima/default/docker.sock")
		if (colimaSocket.exists()) {
			environment("DOCKER_HOST", "unix://${colimaSocket.absolutePath}")
			// Bind-mount sources are resolved inside the Colima VM, where the
			// daemon socket sits at the standard path.
			environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
		}
	}
}
