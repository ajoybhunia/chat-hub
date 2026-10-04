package com.chathub.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * One JSON object per log line, matching the team's TS-service log schema:
 *
 * <pre>
 * {"service":"chat-hub","module":"AuthController","timestamp":"2026-10-04 17:20:11.104","level":"debug","message":"POST : /auth/login"}
 * </pre>
 *
 * {@code module} is the logger's simple class name (last segment of the
 * logger name). {@code details} is omitted when empty; it carries MDC values
 * plus {@code error}/{@code exception}/{@code stack} when the event has a
 * throwable. Throwables stay readable on one line: {@code error} is the
 * concise root-cause message, {@code stack} is bounded (frames and total
 * length are capped, with explicit truncation markers), and newlines in
 * {@code message} are collapsed so each event occupies exactly one line.
 */
public class JsonLineEncoder extends EncoderBase<ILoggingEvent> {

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
			.withZone(ZoneId.systemDefault());

	private static final int MAX_ERROR_CHARS = 500;
	private static final int MAX_STACK_CHARS = 4000;
	private static final int MAX_FRAMES_PER_CAUSE = 20;
	private static final int MAX_CAUSE_DEPTH = 20;

	private final ObjectMapper mapper = new ObjectMapper();

	private volatile String service = "chat-hub";

	public void setService(String service) {
		this.service = service;
	}

	@Override
	public byte[] headerBytes() {
		return null;
	}

	@Override
	public byte[] footerBytes() {
		return null;
	}

	@Override
	public byte[] encode(ILoggingEvent event) {
		try {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("service", service);
			line.put("module", moduleOf(event.getLoggerName()));
			line.put("timestamp", TIMESTAMP.format(Instant.ofEpochMilli(event.getTimeStamp())));
			line.put("level", event.getLevel().toString().toLowerCase(Locale.ROOT));
			line.put("message", flatten(event.getFormattedMessage()));

			Map<String, Object> details = new LinkedHashMap<>();
			Map<String, String> mdc = event.getMDCPropertyMap();
			if (mdc != null) {
				mdc.forEach(details::put);
			}
			IThrowableProxy throwable = event.getThrowableProxy();
			if (throwable != null) {
				IThrowableProxy root = rootCause(throwable);
				details.put("error", truncate(errorDetail(root, throwable), MAX_ERROR_CHARS));
				details.put("exception", root.getClassName());
				details.put("stack", stackTraceOf(throwable));
			}
			if (!details.isEmpty()) {
				line.put("details", details);
			}
			return (mapper.writeValueAsString(line) + "\n").getBytes(StandardCharsets.UTF_8);
		} catch (RuntimeException e) {
			return ("{\"service\":\"" + service + "\",\"module\":\"JsonLineEncoder\",\"level\":\"error\","
					+ "\"message\":\"log serialization failed\"}\n").getBytes(StandardCharsets.UTF_8);
		}
	}

	private static String flatten(String message) {
		if (message == null) {
			return "";
		}
		return message.trim().replaceAll("\\R+", " | ");
	}

	private static IThrowableProxy rootCause(IThrowableProxy throwable) {
		IThrowableProxy root = throwable;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		return root;
	}

	/** Concise one-line description of the root cause (falls back to the top-level throwable). */
	private static String errorDetail(IThrowableProxy root, IThrowableProxy top) {
		if (root.getMessage() != null && !root.getMessage().isBlank()) {
			return root.getMessage().replace('\n', ' ');
		}
		if (top.getMessage() != null && !top.getMessage().isBlank()) {
			return top.getMessage().replace('\n', ' ');
		}
		return root.getClassName();
	}

	private static String moduleOf(String loggerName) {
		if (loggerName == null || loggerName.isEmpty()) {
			return "unknown";
		}
		int dot = loggerName.lastIndexOf('.');
		return dot < 0 ? loggerName : loggerName.substring(dot + 1);
	}

	private static String truncate(String value, int maxChars) {
		return value.length() <= maxChars ? value : value.substring(0, maxChars) + "... [truncated]";
	}

	/** Bounded stack: cause headers always shown, frames capped per cause and in total. */
	private static String stackTraceOf(IThrowableProxy throwable) {
		StringBuilder sb = new StringBuilder();
		appendStack(throwable, sb, 0);
		return sb.toString();
	}

	private static void appendStack(IThrowableProxy throwable, StringBuilder sb, int depth) {
		if (throwable == null) {
			return;
		}
		if (depth >= MAX_CAUSE_DEPTH) {
			if (throwable.getCause() != null) {
				sb.append("\n... [deeper causes suppressed]");
			}
			return;
		}
		if (depth > 0) {
			sb.append("\nCaused by: ");
		}
		sb.append(throwable.getClassName());
		if (throwable.getMessage() != null) {
			sb.append(": ").append(truncate(throwable.getMessage().replace('\n', ' '), MAX_ERROR_CHARS));
		}
		StackTraceElementProxy[] steps = throwable.getStackTraceElementProxyArray();
		if (steps != null) {
			// StackTraceElementProxy.toString() already starts with "at ".
			int shown = 0;
			for (StackTraceElementProxy step : steps) {
				if (shown >= MAX_FRAMES_PER_CAUSE || sb.length() >= MAX_STACK_CHARS) {
					break;
				}
				sb.append("\n\t").append(step);
				shown++;
			}
			if (shown < steps.length) {
				sb.append("\n\t... ").append(steps.length - shown).append(" more frames");
			}
		}
		appendStack(throwable.getCause(), sb, depth + 1);
	}
}
