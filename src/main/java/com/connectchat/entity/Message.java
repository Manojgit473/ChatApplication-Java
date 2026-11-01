package com.connectchat.entity;

import com.connectchat.enums.MessageStatus;
import com.connectchat.enums.MessageType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persisted Message entity.
 * 
 * Demonstrates:
 * - Complex JPA mappings: Self-referencing relationship for threaded replies (@ManyToOne replyTo).
 * - ManyToMany relationship for starred messages (@JoinTable message_stars).
 * - Composite database indexing on (conversation_id, created_at) for fast message history retrieval.
 * - Idempotency key indexing for deduplication.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at"),
    @Index(name = "idx_msg_idempotency", columnList = "idempotency_key")
})
public class Message extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status = MessageStatus.SENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private Message replyTo;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "is_edited", nullable = false)
    private boolean edited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "message_stars",
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> starredByUsers = new HashSet<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageReceipt> receipts = new ArrayList<>();

    public Message() {
    }

    public Message(Conversation conversation, User sender, String content, MessageType type) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.status = MessageStatus.SENT;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
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

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Message getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(Message replyTo) {
        this.replyTo = replyTo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Set<User> getStarredByUsers() {
        return starredByUsers;
    }

    public void setStarredByUsers(Set<User> starredByUsers) {
        this.starredByUsers = starredByUsers;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public List<MessageReceipt> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<MessageReceipt> receipts) {
        this.receipts = receipts;
    }
}

