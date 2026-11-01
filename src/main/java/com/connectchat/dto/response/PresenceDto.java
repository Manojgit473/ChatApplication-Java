package com.connectchat.dto.response;

import com.connectchat.enums.OnlineStatus;
import java.time.Instant;

/**
 * Real-time event DTO for presence updates (ONLINE, AWAY, OFFLINE).
 */
public class PresenceDto {

    private Long userId;
    private String username;
    private OnlineStatus onlineStatus;
    private Instant lastSeenAt;

    public PresenceDto() {
    }

    public PresenceDto(Long userId, String username, OnlineStatus onlineStatus, Instant lastSeenAt) {
        this.userId = userId;
        this.username = username;
        this.onlineStatus = onlineStatus;
        this.lastSeenAt = lastSeenAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OnlineStatus getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(OnlineStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
