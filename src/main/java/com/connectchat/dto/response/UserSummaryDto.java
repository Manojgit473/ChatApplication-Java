package com.connectchat.dto.response;

import com.connectchat.enums.OnlineStatus;
import java.time.Instant;

/**
 * Lightweight User DTO for lists, sender previews, and contact details.
 */
public class UserSummaryDto {

    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String phoneNumber;
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

    public UserSummaryDto(Long id, String username, String displayName, String email, String phoneNumber, String avatarUrl, OnlineStatus onlineStatus, Instant lastSeenAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
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

    public String getName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMobileNumber() {
        return phoneNumber;
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
