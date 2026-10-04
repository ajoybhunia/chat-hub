package com.chathub.common.logging;

import java.util.Map;
import org.slf4j.MDC;

/**
 * Attaches structured key/value pairs to a single log statement — they land in
 * the {@code details} object of the JSON line. Always cleared afterwards.
 *
 * <pre>
 * LogDetails.with(Map.of("userId", user.id()), () -&gt; log.info("Login successful"));
 * </pre>
 */
public final class LogDetails {

	private LogDetails() {
	}

	public static void with(Map<String, ?> details, Runnable logStatement) {
		try {
			details.forEach((key, value) -> {
				if (key != null && value != null) {
					MDC.put(key, String.valueOf(value));
				}
			});
			logStatement.run();
		} finally {
			details.keySet().forEach(MDC::remove);
		}
	}
}
