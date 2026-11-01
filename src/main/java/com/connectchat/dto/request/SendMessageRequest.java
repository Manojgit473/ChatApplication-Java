package com.connectchat.dto.request;

import com.connectchat.enums.MessageType;
import jakarta.validation.constraints.Size;

/**
 * DTO for sending messages over REST or WebSocket STOMP.
 */
public class SendMessageRequest {

    @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
    private String content;

    private MessageType type = MessageType.TEXT;

    private Long replyToId;

    @Size(max = 64, message = "Idempotency key cannot exceed 64 characters")
    private String idempotencyKey;

    public SendMessageRequest() {
    }

    public SendMessageRequest(String content, MessageType type, Long replyToId, String idempotencyKey) {
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
        this.replyToId = replyToId;
        this.idempotencyKey = idempotencyKey;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
