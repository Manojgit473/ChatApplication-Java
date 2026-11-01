package com.connectchat.dto.response;

import com.connectchat.enums.OnlineStatus;
import java.time.Instant;

/**
 * Lightweight User DTO for lists, sender previews, and avatars.
 */
public class UserSummaryDto {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private OnlineStatus onlineStatus;
    private Instant lastSeenAt;

    public UserSummaryDto() {
    }

    public UserSummaryDto(Long id, String username, String displayName, String avatarUrl, OnlineStatus onlineStatus, Instant lastSeenAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.onlineStatus = onlineStatus;
        this.lastSeenAt = lastSeenAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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

