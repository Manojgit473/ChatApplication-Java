package com.connectchat.entity;

import com.connectchat.enums.ConversationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted Conversation entity (handles both Direct and Group chats).
 * 
 * Demonstrates:
 * - @Entity with @Table and indexing on lastMessageAt for performant conversation sorting.
 * - @Enumerated for ConversationType (DIRECT, GROUP).
 * - Bidirectional @OneToMany relationships with CascadeType and orphanRemoval.
 */
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversations_last_msg", columnList = "last_message_at")
})
public class Conversation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConversationType type = ConversationType.DIRECT;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "group_avatar_url", length = 255)
    private String groupAvatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    public Conversation() {
    }

    public Conversation(ConversationType type, String title, User createdBy) {
        this.type = type;
        this.title = title;
        this.createdBy = createdBy;
        this.lastMessageAt = Instant.now();
    }

    public void addMember(ConversationMember member) {
        members.add(member);
        member.setConversation(this);
    }

    public void removeMember(ConversationMember member) {
        members.remove(member);
        member.setConversation(null);
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public List<ConversationMember> getMembers() {
        return members;
    }

    public void setMembers(List<ConversationMember> members) {
        this.members = members;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
