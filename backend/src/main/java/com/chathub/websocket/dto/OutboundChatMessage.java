package com.chathub.websocket.dto;

/** Server → client broadcast frame: {@code {user, content, timestamp}}. */
public record OutboundChatMessage(String user, String content, String timestamp) {
}
