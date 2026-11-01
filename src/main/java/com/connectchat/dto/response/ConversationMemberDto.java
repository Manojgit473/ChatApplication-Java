package com.connectchat.dto.response;

import com.connectchat.enums.GroupRole;
import java.time.Instant;

/**
 * DTO for members in conversation participant lists.
 */
public class ConversationMemberDto {

    private UserSummaryDto user;
    private GroupRole role;
    private Instant joinedAt;

    public ConversationMemberDto() {
    }

    public ConversationMemberDto(UserSummaryDto user, GroupRole role, Instant joinedAt) {
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UserSummaryDto getUser() {
        return user;
    }

    public void setUser(UserSummaryDto user) {
        this.user = user;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
