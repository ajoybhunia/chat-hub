package com.chathub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

class JsonLineEncoderTest {

	private final ObjectMapper mapper = new ObjectMapper();
	private final JsonLineEncoder encoder = new JsonLineEncoder();
	private final List<String> lines = new ArrayList<>();
	private LoggerContext context;
	private Logger logger;

	@BeforeEach
	void attachEncoder() {
		context = (LoggerContext) LoggerFactory.getILoggerFactory();
		logger = context.getLogger("com.chathub.probe.AuthProbe");
		AppenderBase<ILoggingEvent> capturing = new AppenderBase<>() {
			@Override
			protected void append(ILoggingEvent event) {
				lines.add(new String(encoder.encode(event)));
			}
		};
		capturing.start();
		logger.addAppender(capturing);
		logger.setAdditive(false);
		logger.setLevel(Level.DEBUG);
	}

	@AfterEach
	void detach() {
		MDC.clear();
		logger.detachAndStopAllAppenders();
	}

	@Test
	void emitsTeamLogSchema() throws Exception {
		logger.debug("POST : /auth/login");

		var json = mapper.readTree(lines.get(0));
		assertThat(json.get("service").asString()).isEqualTo("chat-hub");
		assertThat(json.get("module").asString()).isEqualTo("AuthProbe");
		assertThat(json.get("level").asString()).isEqualTo("debug");
		assertThat(json.get("message").asString()).isEqualTo("POST : /auth/login");
		assertThat(json.get("timestamp").asString()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
		assertThat(json.has("details")).isFalse();
	}

	@Test
	void carriesMdcValuesAsDetails() throws Exception {
		LogDetails.with(java.util.Map.of("userId", 42, "email", "a@b.com"), () -> logger.info("Login successful"));

		var json = mapper.readTree(lines.get(0));
		assertThat(json.get("message").asString()).isEqualTo("Login successful");
		assertThat(json.get("details").get("userId").asString()).isEqualTo("42");
		assertThat(json.get("details").get("email").asString()).isEqualTo("a@b.com");
		assertThat(MDC.get("userId")).isNull(); // cleared after the statement
	}

	@Test
	void rendersThrowablesWithFullStack() throws Exception {
		logger.error("Unhandled exception", new IllegalStateException("kaboom"));

		var json = mapper.readTree(lines.get(0));
		var details = json.get("details");
		assertThat(details.get("error").asString()).isEqualTo("kaboom");
		assertThat(details.get("exception").asString()).isEqualTo("java.lang.IllegalStateException");
		assertThat(details.get("stack").asString()).contains("java.lang.IllegalStateException: kaboom")
				.contains("at ");
	}

	@Test
	void usesRootCauseAsConciseErrorDetail() throws Exception {
		var wrapped = new IllegalStateException("wrapper happened",
				new IllegalArgumentException("root cause: port 8000 was already in use"));
		logger.error("Application run failed", wrapped);

		var json = mapper.readTree(lines.get(0));
		var details = json.get("details");
		assertThat(details.get("error").asString()).isEqualTo("root cause: port 8000 was already in use");
		assertThat(details.get("exception").asString()).isEqualTo("java.lang.IllegalArgumentException");
		assertThat(details.get("stack").asString()).contains("Caused by: java.lang.IllegalArgumentException")
				.contains("wrapper happened");
	}

	@Test
	void boundsStackTraceInsteadOfWallOfText() throws Exception {
		var wide = new RuntimeException("big");
		StackTraceElement[] frames = new StackTraceElement[500];
		for (int i = 0; i < frames.length; i++) {
			frames[i] = new StackTraceElement("com.chathub.probe.DeepFrame" + i, "invoke", "DeepFrame" + i + ".java", i);
		}
		wide.setStackTrace(frames);
		logger.error("boom", wide);

		var json = mapper.readTree(lines.get(0));
		var stack = json.get("details").get("stack").asString();
		assertThat(stack).contains("more frames");
		assertThat(stack.length()).isLessThan(4200);
	}

	@Test
	void flattensMultilineMessagesToOneLine() throws Exception {
		logger.error("\n***************************\nAPPLICATION FAILED TO START\n\nDescription:\nPort 8000 was already in use.\n");

		var json = mapper.readTree(lines.get(0));
		assertThat(json.get("message").asString())
				.isEqualTo("*************************** | APPLICATION FAILED TO START | Description: | "
						+ "Port 8000 was already in use.");
	}

	@Test
	void lowercasesLevels() throws Exception {
		logger.warn("warn line");
		var json = mapper.readTree(lines.get(0));
		assertThat(json.get("level").asString()).isEqualTo("warn");
	}
}
