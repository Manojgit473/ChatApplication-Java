package com.connectchat.dto.response;

import java.time.Instant;

/**
 * Real-time event DTO for read receipts.
 */
public class ReadReceiptDto {

    private Long conversationId;
    private Long messageId;
    private Long readerId;
    private Instant readAt;

    public ReadReceiptDto() {
    }

    public ReadReceiptDto(Long conversationId, Long messageId, Long readerId, Instant readAt) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.readerId = readerId;
        this.readAt = readAt;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getReaderId() {
        return readerId;
    }

    public void setReaderId(Long readerId) {
        this.readerId = readerId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
