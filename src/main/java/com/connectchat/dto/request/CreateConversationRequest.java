package com.connectchat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO to initiate a 1-on-1 direct conversation.
 */
public class CreateConversationRequest {

    @NotNull(message = "Recipient user ID is required")
    private Long recipientId;

    @Size(max = 2000, message = "Initial message cannot exceed 2000 characters")
    private String initialMessage;

    public CreateConversationRequest() {
    }

    public CreateConversationRequest(Long recipientId, String initialMessage) {
        this.recipientId = recipientId;
        this.initialMessage = initialMessage;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getInitialMessage() {
        return initialMessage;
    }

    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }
}
