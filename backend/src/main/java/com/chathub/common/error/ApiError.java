package com.chathub.common.error;

/**
 * Error response body matching the Deno backend's {@code { error: message }} shape.
 */
public record ApiError(String error) {
}
