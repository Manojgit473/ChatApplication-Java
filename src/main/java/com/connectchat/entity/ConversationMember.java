package com.connectchat.entity;

import com.connectchat.enums.GroupRole;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Membership association entity between User and Conversation.
 * 
 * Demonstrates:
 * - Join entity pattern with rich domain attributes (role, muted, archived, last read).
 * - Multi-column unique constraint to prevent duplicate memberships.
 * - ManyToOne relations with FetchType.LAZY to prevent N+1 select queries.
 */
@Entity
@Table(name = "conversation_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_conversation_user", columnNames = {"conversation_id", "user_id"})
}, indexes = {
    @Index(name = "idx_member_user", columnList = "user_id")
})
public class ConversationMember extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GroupRole role = GroupRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "is_muted", nullable = false)
    private boolean muted = false;

    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    public ConversationMember() {
    }

    public ConversationMember(Conversation conversation, User user, GroupRole role) {
        this.conversation = conversation;
        this.user = user;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
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

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
