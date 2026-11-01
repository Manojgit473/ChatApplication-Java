package com.connectchat.entity;

import jakarta.persistence.*;

/**
 * Persisted BlockedUser entity.
 * 
 * Demonstrates:
 * - Unidirectional blocking relationship with database-level uniqueness.
 * - Bidirectional blocking policy enforcement in services.
 */
@Entity
@Table(name = "blocked_users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
})
public class BlockedUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    public BlockedUser() {
    }

    public BlockedUser(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }

    public User getBlocker() {
        return blocker;
    }

    public void setBlocker(User blocker) {
        this.blocker = blocker;
    }

    public User getBlocked() {
        return blocked;
    }

    public void setBlocked(User blocked) {
        this.blocked = blocked;
    }
}
