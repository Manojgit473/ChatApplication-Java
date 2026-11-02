package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating user profile fields.
 */
public class ProfileUpdateRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String displayName;

    @Size(max = 20, message = "Mobile number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 255, message = "Bio cannot exceed 255 characters")
    private String bio;

    @Size(max = 255, message = "Avatar URL cannot exceed 255 characters")
    private String avatarUrl;

    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String displayName, String bio, String avatarUrl) {
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public ProfileUpdateRequest(String displayName, String phoneNumber, String bio, String avatarUrl) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
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

    public void setName(String name) {
        this.displayName = name;
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

    public void setMobileNumber(String mobileNumber) {
        this.phoneNumber = mobileNumber;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
