package com.connectchat.dto.response;

import com.connectchat.enums.ConversationType;
import java.time.Instant;

/**
 * Summary DTO for displaying conversations in the sidebar list.
 */
public class ConversationSummaryDto {

    private Long id;
    private ConversationType type;
    private String title;
    private String groupAvatarUrl;
    private Instant lastMessageAt;
    private long unreadCount;
    private MessageDto lastMessage;
    private boolean muted;
    private boolean archived;
    private UserSummaryDto directParticipant;
    private int memberCount;

    public ConversationSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGroupAvatarUrl() {
        return groupAvatarUrl;
    }

    public void setGroupAvatarUrl(String groupAvatarUrl) {
        this.groupAvatarUrl = groupAvatarUrl;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public MessageDto getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageDto lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public UserSummaryDto getDirectParticipant() {
        return directParticipant;
    }

    public void setDirectParticipant(UserSummaryDto directParticipant) {
        this.directParticipant = directParticipant;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }
}

