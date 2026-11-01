package com.connectchat.dto.response;

import com.connectchat.enums.ConversationType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed DTO for opened conversation panel.
 */
public class ConversationDetailDto {

    private Long id;
    private ConversationType type;
    private String title;
    private String description;
    private String groupAvatarUrl;
    private UserSummaryDto createdBy;
    private Instant createdAt;
    private Instant lastMessageAt;
    private boolean muted;
    private boolean archived;
    private List<ConversationMemberDto> members = new ArrayList<>();
    private UserSummaryDto directParticipant;

    public ConversationDetailDto() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupAvatarUrl() {
        return groupAvatarUrl;
    }

    public void setGroupAvatarUrl(String groupAvatarUrl) {
        this.groupAvatarUrl = groupAvatarUrl;
    }

    public UserSummaryDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserSummaryDto createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
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

    public List<ConversationMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<ConversationMemberDto> members) {
        this.members = members;
    }

    public UserSummaryDto getDirectParticipant() {
        return directParticipant;
    }

    public void setDirectParticipant(UserSummaryDto directParticipant) {
        this.directParticipant = directParticipant;
    }
}
