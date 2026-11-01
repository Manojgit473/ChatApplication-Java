package com.connectchat.dto.request;

/**
 * DTO for real-time typing indicators sent via WebSocket STOMP.
 */
public class TypingNotificationRequest {

    private boolean typing;

    public TypingNotificationRequest() {
    }

    public TypingNotificationRequest(boolean typing) {
        this.typing = typing;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
