package com.connectchat.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persisted MessageReceipt entity for tracking delivery and read timestamps per recipient.
 * 
 * Demonstrates:
 * - Granular per-user receipt tracking in 1-on-1 and group conversations.
 * - Multi-column unique constraint ensuring one receipt record per user per message.
 */
@Entity
@Table(name = "message_receipts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_receipt_msg_user", columnNames = {"message_id", "user_id"})
})
public class MessageReceipt extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    public MessageReceipt() {
    }

    public MessageReceipt(Message message, User user) {
        this.message = message;
        this.user = user;
        this.deliveredAt = Instant.now();
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}

