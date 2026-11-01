package com.connectchat.dto.response;

/**
 * Real-time event DTO for typing status broadcasts.
 */
public class TypingIndicatorDto {

    private Long conversationId;
    private UserSummaryDto user;
    private boolean typing;

    public TypingIndicatorDto() {
    }

    public TypingIndicatorDto(Long conversationId, UserSummaryDto user, boolean typing) {
        this.conversationId = conversationId;
        this.user = user;
        this.typing = typing;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public UserSummaryDto getUser() {
        return user;
    }

    public void setUser(UserSummaryDto user) {
        this.user = user;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}

