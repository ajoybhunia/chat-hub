package com.chathub.common.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * ISO-8601 UTC timestamps with millisecond precision — matches JavaScript's
 * {@code Date.toISOString()} exactly (e.g. {@code 2026-09-27T12:34:56.789Z}),
 * unlike {@link Instant#toString()} which omits trailing zeros.
 */
public final class IsoTimestamps {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	private IsoTimestamps() {
	}

	public static String now() {
		return FORMATTER.format(Instant.now());
	}
}
