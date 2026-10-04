package com.chathub.websocket.dto;

/** Client → server chat frame: {@code {user?, content}}. */
public record InboundChatMessage(String user, String content) {
}
