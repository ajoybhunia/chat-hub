package com.chathub.common.error;

import com.chathub.common.logging.LogDetails;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps exceptions to the JSON error contract used by the frontend
 * ( {@code {error: message}}). Mirrors the Deno backend where possible:
 * business failures → 400, unknown routes → 404 "Not found". Every rejection
 * is logged with structured {@code details} for the JSON log stream.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({ DuplicateResourceException.class, InvalidCredentialsException.class })
	public ResponseEntity<ApiError> badRequest(RuntimeException exception) {
		warnRejected(HttpStatus.BAD_REQUEST, exception.getMessage());
		return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
				.map(error -> error.getField() + " " + error.getDefaultMessage()).orElse("Invalid request");
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("status", HttpStatus.BAD_REQUEST.value());
		details.put("error", message);
		details.put("fields", exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).toList().toString());
		LogDetails.with(details, () -> log.warn("Request rejected : validation failed"));
		return ResponseEntity.badRequest().body(new ApiError(message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException exception) {
		warnRejected(HttpStatus.BAD_REQUEST, "Invalid request body");
		return ResponseEntity.badRequest().body(new ApiError("Invalid request body"));
	}

	// Deno answers wrong-method requests under /auth/* with 404 "Not found"
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("status", HttpStatus.NOT_FOUND.value());
		details.put("error", "Not found");
		details.put("method", exception.getMethod());
		LogDetails.with(details, () -> log.debug("Request rejected : method not supported"));
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("Not found"));
	}

	@ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
	public ResponseEntity<ApiError> notFound(Exception exception) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("status", HttpStatus.NOT_FOUND.value());
		details.put("error", "Not found");
		LogDetails.with(details, () -> log.debug("Request rejected : no handler"));
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("Not found"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> unexpected(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiError("Internal server error"));
	}

	private static void warnRejected(HttpStatus status, String error) {
		LogDetails.with(Map.of("status", status.value(), "error", error),
				() -> log.warn("Request rejected"));
	}
}
